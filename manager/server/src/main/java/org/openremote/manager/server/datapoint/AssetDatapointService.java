package org.openremote.manager.server.datapoint;

import org.hibernate.Session;
import org.hibernate.jdbc.AbstractReturningWork;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.model.AttributeRef;
import org.openremote.model.asset.AssetState;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.postgresql.util.PGInterval;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Store and retrieve datapoints for asset attributes.
 */
public class AssetDatapointService implements ContainerService, Consumer<AssetState> {

    private static final Logger LOG = Logger.getLogger(AssetDatapointService.class.getName());

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new AssetDatapointResourceImpl(
                container.getService(ManagerIdentityService.class),
                container.getService(AssetStorageService.class),
                this
            )
        );

    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public void accept(AssetState assetState) {
        if (assetState.getAttribute().isStoreDatapoints()) {
            LOG.finest("Storing data point for: " + assetState);
            AssetDatapoint assetDatapoint = new AssetDatapoint(assetState.getAttribute().getStateEvent());
            persistenceService.doTransaction(entityManager -> entityManager.persist(assetDatapoint));
        } else {
            LOG.finest("Ignoring as attribute is not a data point: " + assetState);
        }
    }

    public List<AssetDatapoint> getDatapoints(AttributeRef attributeRef) {
        return persistenceService.doReturningTransaction(entityManager -> entityManager.createQuery(
            "select dp from AssetDatapoint dp " +
                "where dp.entityId = :assetId " +
                "and dp.attributeName = :attributeName " +
                "order by dp.timestamp desc",
            AssetDatapoint.class)
            .setParameter("assetId", attributeRef.getEntityId())
            .setParameter("attributeName", attributeRef.getAttributeName())
            .getResultList());
    }

    public NumberDatapoint[] aggregateDatapoints(AttributeRef attributeRef,
                                                 DatapointInterval datapointInterval,
                                                 long timestamp) {
        return persistenceService.doReturningTransaction(entityManager ->
            entityManager.unwrap(Session.class).doReturningWork(new AbstractReturningWork<NumberDatapoint[]>() {
                @Override
                public NumberDatapoint[] execute(Connection connection) throws SQLException {

                    String truncateX;
                    String step;
                    String interval;
                    Function<Timestamp, String> labelFunction;

                    SimpleDateFormat dayFormat = new SimpleDateFormat("dd. MMM yyyy");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                    switch (datapointInterval) {
                        case HOUR:
                            truncateX = "minute";
                            step = "1 minute";
                            interval = "1 hour";
                            labelFunction = timeFormat::format;
                            break;
                        case DAY:
                            truncateX = "hour";
                            step = "1 hour";
                            interval = "1 day";
                            labelFunction = timeFormat::format;
                            break;
                        case WEEK:
                            truncateX = "day";
                            step = "1 day";
                            interval = "7 day";
                            labelFunction = dayFormat::format;
                            break;
                        case MONTH:
                            truncateX = "day";
                            step = "1 day";
                            interval = "1 month";
                            labelFunction = dayFormat::format;
                            break;
                        case YEAR:
                            truncateX = "month";
                            step = "1 month";
                            interval = "1 year";
                            labelFunction = dayFormat::format;
                            break;
                        default:
                            throw new IllegalArgumentException("Can't handle interval: " + datapointInterval);
                    }

                    PreparedStatement st = connection.prepareStatement(
                        "select TS as X, coalesce(AVG_VALUE, 0) as Y " +
                            " from ( " +
                            "       select date_trunc(?, GS)::timestamp TS " +
                            "       from generate_series(to_timestamp(?) - ?, to_timestamp(?), ?) GS " +
                            "       ) TS " +
                            "  left join ( " +
                            "       select " +
                            "           date_trunc(?, to_timestamp(TIMESTAMP / 1000))::timestamp as TS, " +
                            "           AVG(VALUE::text::numeric) as AVG_VALUE " +
                            "         from ASSET_DATAPOINT " +
                            "         where " +
                            "           to_timestamp(TIMESTAMP / 1000) >= to_timestamp(?) - ? " +
                            "           and " +
                            "           to_timestamp(TIMESTAMP / 1000) <= to_timestamp(?) " +
                            "           and " +
                            "           ENTITY_ID = ? and ATTRIBUTE_NAME = ? " +
                            "         group by TS " +
                            "  ) DP using (TS) " +
                            " order by TS asc "
                    );

                    long timestampSeconds = timestamp / 1000;
                    st.setString(1, truncateX);
                    st.setLong(2, timestampSeconds);
                    st.setObject(3, new PGInterval(interval));
                    st.setLong(4, timestampSeconds);
                    st.setObject(5, new PGInterval(step));
                    st.setString(6, truncateX);
                    st.setLong(7, timestampSeconds);
                    st.setObject(8, new PGInterval(interval));
                    st.setLong(9, timestampSeconds);
                    st.setString(10, attributeRef.getEntityId());
                    st.setString(11, attributeRef.getAttributeName());

                    try (ResultSet rs = st.executeQuery()) {
                        List<NumberDatapoint> result = new ArrayList<>();
                        while (rs.next()) {
                            result.add(new NumberDatapoint(
                                    labelFunction.apply(rs.getTimestamp(1)),
                                    rs.getBigDecimal(2)
                                )
                            );
                        }
                        return result.toArray(new NumberDatapoint[result.size()]);
                    }
                }
            })
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}

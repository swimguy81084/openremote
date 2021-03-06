/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client;

import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.admin.overview.AdminOverviewActivity;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantActivity;
import org.openremote.manager.client.admin.tenant.AdminTenantPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantsActivity;
import org.openremote.manager.client.admin.tenant.AdminTenantsPlace;
import org.openremote.manager.client.admin.users.AdminUserActivity;
import org.openremote.manager.client.admin.users.AdminUserPlace;
import org.openremote.manager.client.admin.users.AdminUsersActivity;
import org.openremote.manager.client.admin.users.AdminUsersPlace;
import org.openremote.manager.client.apps.AppsActivity;
import org.openremote.manager.client.apps.AppsPlace;
import org.openremote.manager.client.assets.AssetsDashboardActivity;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.asset.AssetEditActivity;
import org.openremote.manager.client.assets.asset.AssetEditPlace;
import org.openremote.manager.client.assets.asset.AssetViewActivity;
import org.openremote.manager.client.assets.asset.AssetViewPlace;
import org.openremote.manager.client.assets.tenant.AssetsTenantActivity;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.model.event.bus.EventBus;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.map.MapActivity;
import org.openremote.manager.client.map.MapPlace;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.mvp.AppActivityMapper;
import org.openremote.manager.client.mvp.RoleRequiredException;
import org.openremote.manager.client.rules.asset.AssetRulesEditorActivity;
import org.openremote.manager.client.rules.asset.AssetRulesEditorPlace;
import org.openremote.manager.client.rules.asset.AssetRulesListActivity;
import org.openremote.manager.client.rules.asset.AssetRulesListPlace;
import org.openremote.manager.client.rules.global.GlobalRulesEditorActivity;
import org.openremote.manager.client.rules.global.GlobalRulesEditorPlace;
import org.openremote.manager.client.rules.global.GlobalRulesListActivity;
import org.openremote.manager.client.rules.global.GlobalRulesListPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesEditorActivity;
import org.openremote.manager.client.rules.tenant.TenantRulesEditorPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesListActivity;
import org.openremote.manager.client.rules.tenant.TenantRulesListPlace;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.user.UserAccountActivity;
import org.openremote.manager.client.user.UserAccountPlace;

import java.util.logging.Logger;

public class ManagerActivityMapper implements AppActivityMapper {

    private static final Logger LOG = Logger.getLogger(ManagerActivityMapper.class.getName());

    protected final SecurityService securityService;
    protected final EventBus eventBus;
    protected final ManagerMessages managerMessages;
    protected final Provider<AssetsDashboardActivity> assetsDashboardActivityProvider;
    protected final Provider<AssetsTenantActivity> assetsTenantActivityProvider;
    protected final Provider<AssetViewActivity> assetViewActivityProvider;
    protected final Provider<AssetEditActivity> assetEditActivityProvider;
    protected final Provider<MapActivity> mapActivityProvider;
    protected final Provider<GlobalRulesListActivity> globalRulesActivityProvider;
    protected final Provider<GlobalRulesEditorActivity> globalRulesEditorActivityProvider;
    protected final Provider<TenantRulesListActivity> tenantRulesListActivityProvider;
    protected final Provider<TenantRulesEditorActivity> tenantRulesEditorActivityProvider;
    protected final Provider<AssetRulesListActivity> assetRulesListActivityProvider;
    protected final Provider<AssetRulesEditorActivity> assetRulesEditorActivityProvider;
    protected final Provider<AppsActivity> appsActivityProvider;
    protected final Provider<AdminOverviewActivity> adminOverviewActivityProvider;
    protected final Provider<AdminTenantsActivity> adminTenantsActivityProvider;
    protected final Provider<AdminTenantActivity> adminTenantActivityProvider;
    protected final Provider<AdminUsersActivity> adminUsersActivityProvider;
    protected final Provider<AdminUserActivity> adminUserActivityProvider;
    protected final Provider<UserAccountActivity> userProfileActivityProvider;

    @Inject
    public ManagerActivityMapper(SecurityService securityService,
                                 EventBus eventBus,
                                 ManagerMessages managerMessages,
                                 Provider<AssetsDashboardActivity> assetsDashboardActivityProvider,
                                 Provider<AssetsTenantActivity> assetsTenantActivityProvider,
                                 Provider<AssetViewActivity> assetViewActivityProvider,
                                 Provider<AssetEditActivity> assetEditActivityProvider,
                                 Provider<MapActivity> mapActivityProvider,
                                 Provider<GlobalRulesListActivity> globalRulesActivityProvider,
                                 Provider<GlobalRulesEditorActivity> globalRulesEditorActivityProvider,
                                 Provider<TenantRulesListActivity> tenantRulesListActivityProvider,
                                 Provider<TenantRulesEditorActivity> tenantRulesEditorActivityProvider,
                                 Provider<AssetRulesListActivity> assetRulesListActivityProvider,
                                 Provider<AssetRulesEditorActivity> assetRulesEditorActivityProvider,
                                 Provider<AppsActivity> appsActivityProvider,
                                 Provider<AdminOverviewActivity> adminOverviewActivityProvider,
                                 Provider<AdminTenantsActivity> adminTenantsActivityProvider,
                                 Provider<AdminTenantActivity> adminTenantActivityProvider,
                                 Provider<AdminUsersActivity> adminUsersActivityProvider,
                                 Provider<AdminUserActivity> adminUserActivityProvider,
                                 Provider<UserAccountActivity> userProfileActivityProvider) {
        this.securityService = securityService;
        this.eventBus = eventBus;
        this.managerMessages = managerMessages;
        this.assetsDashboardActivityProvider = assetsDashboardActivityProvider;
        this.assetsTenantActivityProvider = assetsTenantActivityProvider;
        this.assetViewActivityProvider = assetViewActivityProvider;
        this.assetEditActivityProvider = assetEditActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
        this.globalRulesActivityProvider = globalRulesActivityProvider;
        this.globalRulesEditorActivityProvider = globalRulesEditorActivityProvider;
        this.tenantRulesListActivityProvider = tenantRulesListActivityProvider;
        this.tenantRulesEditorActivityProvider = tenantRulesEditorActivityProvider;
        this.assetRulesListActivityProvider = assetRulesListActivityProvider;
        this.assetRulesEditorActivityProvider = assetRulesEditorActivityProvider;
        this.appsActivityProvider = appsActivityProvider;
        this.adminOverviewActivityProvider = adminOverviewActivityProvider;
        this.adminTenantsActivityProvider = adminTenantsActivityProvider;
        this.adminTenantActivityProvider = adminTenantActivityProvider;
        this.adminUsersActivityProvider = adminUsersActivityProvider;
        this.adminUserActivityProvider = adminUserActivityProvider;
        this.userProfileActivityProvider = userProfileActivityProvider;
    }

    public AppActivity getActivity(Place place) {
        try {
            if (place instanceof AssetsDashboardPlace) {
                return assetsDashboardActivityProvider.get().init(securityService, (AssetsDashboardPlace) place);
            }
            if (place instanceof AssetsTenantPlace) {
                return assetsTenantActivityProvider.get().init(securityService, (AssetsTenantPlace) place);
            }
            if (place instanceof AssetViewPlace) {
                return assetViewActivityProvider.get().init(securityService, (AssetViewPlace) place);
            }
            if (place instanceof AssetEditPlace) {
                return assetEditActivityProvider.get().init(securityService, (AssetEditPlace) place);
            }
            if (place instanceof MapPlace) {
                return mapActivityProvider.get().init(securityService, (MapPlace) place);
            }
            if (place instanceof GlobalRulesListPlace) {
                return globalRulesActivityProvider.get().init(securityService, (GlobalRulesListPlace) place);
            }
            if (place instanceof GlobalRulesEditorPlace) {
                return globalRulesEditorActivityProvider.get().init(securityService, (GlobalRulesEditorPlace) place);
            }
            if (place instanceof TenantRulesListPlace) {
                return tenantRulesListActivityProvider.get().init(securityService, (TenantRulesListPlace) place);
            }
            if (place instanceof TenantRulesEditorPlace) {
                return tenantRulesEditorActivityProvider.get().init(securityService, (TenantRulesEditorPlace) place);
            }
            if (place instanceof AssetRulesListPlace) {
                return assetRulesListActivityProvider.get().init(securityService, (AssetRulesListPlace) place);
            }
            if (place instanceof AssetRulesEditorPlace) {
                return assetRulesEditorActivityProvider.get().init(securityService, (AssetRulesEditorPlace) place);
            }
            if (place instanceof AppsPlace) {
                return appsActivityProvider.get().init(securityService, (AppsPlace) place);
            }
            if (place instanceof AdminOverviewPlace) {
                return adminOverviewActivityProvider.get().init(securityService, (AdminOverviewPlace) place);
            }
            if (place instanceof AdminTenantsPlace) {
                return adminTenantsActivityProvider.get().init(securityService, (AdminTenantsPlace) place);
            }
            if (place instanceof AdminTenantPlace) {
                return adminTenantActivityProvider.get().init(securityService, (AdminTenantPlace) place);
            }
            if (place instanceof AdminUsersPlace) {
                return adminUsersActivityProvider.get().init(securityService, (AdminUsersPlace) place);
            }
            if (place instanceof AdminUserPlace) {
                return adminUserActivityProvider.get().init(securityService, (AdminUserPlace) place);
            }
            if (place instanceof UserAccountPlace) {
                return userProfileActivityProvider.get().init(securityService, (UserAccountPlace) place);
            }

            LOG.severe("No activity available for place: " + place);

        } catch (RoleRequiredException ex) {
            LOG.warning("Access denied, missing required role '" + ex.getRequiredRole() + "': " + place);
            eventBus.dispatch(new ShowFailureEvent(managerMessages.accessDenied(), 5000));
        }
        return null;
    }
}

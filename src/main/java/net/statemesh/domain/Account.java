package net.statemesh.domain;

import net.statemesh.domain.enumeration.ColorScheme;
import net.statemesh.domain.enumeration.MenuMode;
import net.statemesh.domain.enumeration.UserType;
import net.statemesh.service.dto.ProjectId;
import net.statemesh.service.dto.ZoneDTO;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

public interface Account extends Serializable {
    String getId();
    String getLogin();
    String getFullName();
    boolean isActivated();
    String getImageUrl();
    String getLangKey();
    String getCreatedBy();
    Instant getCreatedDate();
    String getLastModifiedBy();
    String getFirstName();
    String getTheme();
    ColorScheme getColorScheme();
    Integer getScale();
    MenuMode getMenuMode();
    Boolean getRipple();
    String getLastName();
    Instant getLastModifiedDate();
    Set<Authority> getAuthorities();
    Set<ProjectId> getProjectIds();
    ZoneDTO getDefaultZone();
    Instant getLockedUserTime();
    Boolean lockedOperator();
    boolean hasApps();
    boolean cicdPipelineAutopublish();
    String getPaymentMethod();
    Double getCredits();
    Boolean getDataCenter();
    UserType getUserType();
    String getCompany();
    String getTaxCode();
    String getAddress();
    String getCity();
    String getState();
    String getCountry();
    String getZip();
    String getReferralCode();
    String getReferredByCode();
}

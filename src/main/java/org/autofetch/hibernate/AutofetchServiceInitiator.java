package org.autofetch.hibernate;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

final class AutofetchServiceInitiator implements StandardServiceInitiator<AutofetchService> {

    static final AutofetchServiceInitiator INSTANCE = new AutofetchServiceInitiator();

    @Override
    public AutofetchService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new AutofetchServiceImpl();
    }

    @Override
    public Class<AutofetchService> getServiceInitiated() {
        return AutofetchService.class;
    }
}

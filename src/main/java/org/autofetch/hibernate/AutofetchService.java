package org.autofetch.hibernate;

import org.hibernate.service.Service;

import java.io.Serializable;

public interface AutofetchService extends Service, Serializable {

    ExtentManager getExtentManager();
}

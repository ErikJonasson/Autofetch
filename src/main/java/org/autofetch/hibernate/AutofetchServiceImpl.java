package org.autofetch.hibernate;

final class AutofetchServiceImpl implements AutofetchService {

    private final ExtentManager extentManager;

    AutofetchServiceImpl() {
        this.extentManager = new ExtentManager();
    }

    @Override
    public ExtentManager getExtentManager() {
        return extentManager;
    }
}

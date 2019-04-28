package org.moera.node.global;

import org.moera.node.option.Options;

public interface RequestContext {

    boolean isBrowserExtension();

    void setBrowserExtension(boolean browserExtension);

    boolean isRootAdmin();

    void setRootAdmin(boolean rootAdmin);

    boolean isAdmin();

    void setAdmin(boolean admin);

    Options getOptions();

    void setOptions(Options options);

    RequestContext getPublic();

}

package dev.identra.app.x.core.settings.interfaces;

import java.util.List;

import dev.identra.app.x.data.interfaces.IValidator;
import dev.identra.app.x.core.settings.NameInformation;

public interface INameInformation extends IValidator {
    boolean hasNameInformation();
    NameInformation getNameInformation();

    int getIndex();
    String getName();
    String getNameNice();
    String getNameNiceNoNumericEnding();

    String getGroup();

    boolean hasParent();
    boolean hasChildren();
    List<NameInformation> getChildrenNames();

    NameInformation getParentNameInformation();
    String getParentName();
    String getParentNameNice();
    String getParentNameNiceNoNumericEnding();
}

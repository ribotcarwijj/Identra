package dev.identra.app.x.core.settings.interfaces;

import dev.identra.app.x.data.interfaces.IValidator;
import dev.identra.app.x.core.settings.NameInformation;

public interface IValueDescriptor extends IValidator {
    String getName();
    String getNameNice();
    String getNameNiceNoNumericEnding();
    NameInformation getNameInformation();

    String getValue();
    String getNewValue();
    String getDescription();
}

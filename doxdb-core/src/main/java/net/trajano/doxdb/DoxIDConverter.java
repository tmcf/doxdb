package net.trajano.doxdb;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class DoxIDConverter implements AttributeConverter<DoxID, String> {

    @Override
    public String convertToDatabaseColumn(DoxID doxid) {

        return doxid.toString();
    }

    @Override
    public DoxID convertToEntityAttribute(String val) {

        return new DoxID(val);
    }

}

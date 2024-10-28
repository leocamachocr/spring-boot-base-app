package dev.leocamacho.demo.tests.api.steps;

import io.cucumber.datatable.DataTable;

import java.lang.reflect.Constructor;
import java.util.Map;

import static dev.leocamacho.demo.tests.fakers.ValueGenerator.generateValue;

public class CucumberAdapters {

    public static Map<String, Object> getSingleRow(DataTable dataTable) {
        return dataTable.asMaps(String.class, Object.class).getFirst();
    }


    public static <R> R mapToInstance(Class<R> type, Map<String, Object> record) {
        try {
            if (type.isRecord()) {
                Constructor<R> constructor = (Constructor<R>) type.getDeclaredConstructors()[0];
                Object[] args = new Object[constructor.getParameterCount()];

                for (int i = 0; i < constructor.getParameters().length; i++) {
                    var param = constructor.getParameters()[i];
                    var value = record.get(param.getName());
                    if (value instanceof String) {
                        args[i] = generateValue((String) value);
                    } else {
                        args[i] = record.get(param.getName());
                    }
                }

                return constructor.newInstance(args);
            } else {
                throw new IllegalArgumentException("Class " + type.getName() + " is not a record class");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map record to instance", e);
        }
    }
}

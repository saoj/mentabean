package org.mentabean.type;

public class EnumValueTypeFactory {
	
	private static EnumValueTypeFactory instance = null;
	
	private EnumValueTypeFactory() {

	}
	
	public static EnumValueTypeFactory getInstance() {
		if (instance == null) {
			instance = new EnumValueTypeFactory();
		}
		return instance;
	}
	
	public EnumValueType from(final Class<? extends Enum<?>> enumType) {
		return new EnumValueType(enumType);
	}
}
package org.mentabean.type;

public class EnumIdTypeFactory {
	
	private static EnumIdTypeFactory instance = null;
	
	private EnumIdTypeFactory() {

	}
	
	public static EnumIdTypeFactory getInstance() {
		if (instance == null) {
			instance = new EnumIdTypeFactory();
		}
		return instance;
	}
	
	public EnumIdType from(final Class<? extends Enum<?>> enumType) {
		return new EnumIdType(enumType);
	}
}
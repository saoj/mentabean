/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * MentaBean => http://www.mentabean.org
 * Author: Sergio Oliveira Jr. (sergio.oliveira.jr@gmail.com)
 */
package org.mentabean;

import java.math.BigDecimal;
import java.util.Date;

import org.mentabean.type.AutoIncrementType;
import org.mentabean.type.AutoTimestampType;
import org.mentabean.type.BigDecimalType;
import org.mentabean.type.BooleanIntType;
import org.mentabean.type.BooleanStringType;
import org.mentabean.type.BooleanType;
import org.mentabean.type.ByteArrayType;
import org.mentabean.type.DateType;
import org.mentabean.type.DoubleType;
import org.mentabean.type.EnumIdTypeFactory;
import org.mentabean.type.EnumValueTypeFactory;
import org.mentabean.type.FloatType;
import org.mentabean.type.GenericType;
import org.mentabean.type.IntegerType;
import org.mentabean.type.LongType;
import org.mentabean.type.NowOnInsertAndUpdateTimestampType;
import org.mentabean.type.NowOnInsertTimestampType;
import org.mentabean.type.NowOnUpdateTimestampType;
import org.mentabean.type.SequenceType;
import org.mentabean.type.StringType;
import org.mentabean.type.TimeType;
import org.mentabean.type.TimestampType;

public class DBTypes {

	public static StringType STRING = new StringType();
	public static IntegerType INTEGER = new IntegerType();
	public static DateType DATE = new DateType();
	public static SequenceType SEQUENCE = new SequenceType();
	public static AutoIncrementType AUTOINCREMENT = new AutoIncrementType();
	public static AutoTimestampType AUTOTIMESTAMP = new AutoTimestampType();
	public static LongType LONG = new LongType();
	public static DoubleType DOUBLE = new DoubleType();
	public static TimeType TIME = new TimeType();
	public static TimestampType TIMESTAMP = new TimestampType();
	public static FloatType FLOAT = new FloatType();
	public static BooleanType BOOLEAN = new BooleanType();
	public static BooleanStringType BOOLEANSTRING = new BooleanStringType();
	public static BooleanIntType BOOLEANINT = new BooleanIntType();
	public static BigDecimalType BIGDECIMAL = new BigDecimalType();
	public static NowOnUpdateTimestampType NOW_ON_UPDATE_TIMESTAMP = new NowOnUpdateTimestampType();
	public static NowOnInsertTimestampType NOW_ON_INSERT_TIMESTAMP = new NowOnInsertTimestampType();
	public static NowOnInsertAndUpdateTimestampType NOW_ON_BOTH_TIMESTAMP = new NowOnInsertAndUpdateTimestampType();
	public static EnumValueTypeFactory ENUMVALUE = EnumValueTypeFactory.getInstance();
	public static EnumIdTypeFactory ENUMID = EnumIdTypeFactory.getInstance();
	public static ByteArrayType BYTE_ARRAY = new ByteArrayType();
	public static GenericType GENERIC = new GenericType();

	@SuppressWarnings("unchecked")
	public static DBType<?> from(Class<? extends Object> klass) {
		if (klass.isInterface()) {
			return null;
		} else if (klass.equals(String.class)) {
			return STRING;
		} else if (klass.equals(Integer.class) || klass.equals(int.class)) {
			return INTEGER;
		} else if (klass.equals(Date.class)) {
			return TIMESTAMP;
		} else if (klass.equals(Long.class) || klass.equals(long.class)) {
			return LONG;
		} else if (klass.equals(Double.class) || klass.equals(double.class)) {
			return DOUBLE;
		} else if (klass.equals(Float.class) || klass.equals(float.class)) {
			return FLOAT;
		} else if (klass.equals(Boolean.class) || klass.equals(boolean.class)) {
			return BOOLEANSTRING;
		} else if (klass.equals(BigDecimal.class)) {
			return BIGDECIMAL;
		} else if (klass.isEnum()) {
			return ENUMVALUE.from((Class<? extends Enum<?>>) klass);
		} else if (klass.equals(byte[].class)) {
			return BYTE_ARRAY;
		}
		return GENERIC;
	}

}

/**
 * Copyright (c) 2006, Gaudenz Alder
 */
package com.mxgraph.io;


import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Collection;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import org.w3c.dom.Node;

public class mxObjectCodecProduct {
	private Map<String, Method> accessors;
	private Map<Class, Map<String, Field>> fields;

	/**
	* Returns the accessor (getter, setter) for the specified field.
	*/
	public Method getAccessor(Object obj, Field field, boolean isGetter) {
		String name = field.getName();
		name = name.substring(0, 1).toUpperCase() + name.substring(1);
		if (!isGetter) {
			name = "set" + name;
		} else if (boolean.class.isAssignableFrom(field.getType())) {
			name = "is" + name;
		} else {
			name = "get" + name;
		}
		Method method = (accessors != null) ? accessors.get(name) : null;
		if (method == null) {
			try {
				if (isGetter) {
					method = getMethod(obj, name, null);
				} else {
					method = getMethod(obj, name, new Class[] { field.getType() });
				}
			} catch (Exception e) {
				mxObjectCodec.log.log(Level.SEVERE, "Failed to get method " + name + " from " + obj, e);
			}
			if (method != null) {
				if (accessors == null) {
					accessors = new Hashtable<String, Method>();
				}
				accessors.put(name, method);
			}
		}
		if (method == null) {
			if (mxObjectCodec.log.isLoggable(Level.FINER))
				mxObjectCodec.log.finer("Failed to find accessor for " + field + " in " + obj);
		}
		return method;
	}

	/**
	* Returns the method with the specified signature.
	*/
	public Method getMethod(Object obj, String methodname, Class[] params) {
		Class<?> type = obj.getClass();
		while (type != null) {
			try {
				Method method = type.getDeclaredMethod(methodname, params);
				if (method != null) {
					return method;
				}
			} catch (Exception e) {
				mxObjectCodec.log.log(Level.FINEST, "Failed to get method " + methodname + " in class " + type, e);
			}
			type = type.getSuperclass();
		}
		return null;
	}

	/**
	* Returns the value of the field using the accessor for the field if one exists.
	*/
	public Object getFieldValueWithAccessor(Object obj, Field field) {
		Object value = null;
		if (field != null) {
			try {
				Method method = getAccessor(obj, field, true);
				if (method != null) {
					value = method.invoke(obj, (Object[]) null);
				}
			} catch (Exception e) {
				mxObjectCodec.log.log(Level.SEVERE, "Failed to get value from field " + field + " in " + obj, e);
			}
		}
		return value;
	}

	/**
	* Converts XML attribute values to object of the given type.
	*/
	public Object convertValueFromXml(Class<?> type, Object value) {
		if (value instanceof String) {
			String tmp = (String) value;
			if (type.equals(boolean.class) || type == Boolean.class) {
				if (tmp.equals("1") || tmp.equals("0")) {
					tmp = (tmp.equals("1")) ? "true" : "false";
				}
				value = Boolean.valueOf(tmp);
			} else if (type.equals(char.class) || type == Character.class) {
				value = Character.valueOf(tmp.charAt(0));
			} else if (type.equals(byte.class) || type == Byte.class) {
				value = Byte.valueOf(tmp);
			} else if (type.equals(short.class) || type == Short.class) {
				value = Short.valueOf(tmp);
			} else if (type.equals(int.class) || type == Integer.class) {
				value = Integer.valueOf(tmp);
			} else if (type.equals(long.class) || type == Long.class) {
				value = Long.valueOf(tmp);
			} else if (type.equals(float.class) || type == Float.class) {
				value = Float.valueOf(tmp);
			} else if (type.equals(double.class) || type == Double.class) {
				value = Double.valueOf(tmp);
			}
		}
		return value;
	}

	/**
	* Sets the value of the given field using the accessor if one exists.
	*/
	public void setFieldValueWithAccessor(Object obj, Field field, Object value) {
		if (field != null) {
			try {
				Method method = getAccessor(obj, field, false);
				if (method != null) {
					Class<?> type = method.getParameterTypes()[0];
					value = convertValueFromXml(type, value);
					if (type.isArray() && value instanceof Collection) {
						Collection<?> coll = (Collection<?>) value;
						value = coll.toArray((Object[]) Array.newInstance(type.getComponentType(), coll.size()));
					}
					method.invoke(obj, new Object[] { value });
				}
			} catch (Exception e) {
				mxObjectCodec.log.log(Level.SEVERE,
						"setFieldValue: " + e + " on " + obj.getClass().getSimpleName() + "." + field.getName() + " ("
								+ field.getType().getSimpleName() + ") = " + value + " ("
								+ value.getClass().getSimpleName() + ")",
						e);
			}
		}
	}

	/**
	* Returns the value of the field with the specified name in the specified object instance.
	*/
	public Object getFieldValue(Object obj, String fieldname) {
		Object value = null;
		if (obj != null && fieldname != null) {
			Field field = getField(obj, fieldname);
			try {
				if (field != null) {
					if (Modifier.isPublic(field.getModifiers())) {
						value = field.get(obj);
					} else {
						value = getFieldValueWithAccessor(obj, field);
					}
				}
			} catch (IllegalAccessException e1) {
				value = getFieldValueWithAccessor(obj, field);
			} catch (Exception e) {
				mxObjectCodec.log.log(Level.SEVERE, "Failed to get value from field " + fieldname + " in " + obj, e);
			}
		}
		return value;
	}

	/**
	* Returns the field with the specified name.
	*/
	public Field getField(Object obj, String fieldname) {
		Class<?> type = obj.getClass();
		if (fields == null) {
			fields = new HashMap<Class, Map<String, Field>>();
		}
		Map<String, Field> map = fields.get(type);
		if (map == null) {
			map = new HashMap<String, Field>();
			fields.put(type, map);
		}
		Field field = map.get(fieldname);
		if (field != null) {
			return field;
		}
		while (type != null) {
			try {
				field = type.getDeclaredField(fieldname);
				if (field != null) {
					map.put(fieldname, field);
					return field;
				}
			} catch (Exception e) {
				mxObjectCodec.log.log(Level.FINEST, "Failed to get field " + fieldname + " in class " + type, e);
			}
			type = type.getSuperclass();
		}
		mxObjectCodec.log.severe("Field " + fieldname + " not found in " + obj);
		return null;
	}

	/**
	* Sets the value of the field with the specified name in the specified object instance.
	*/
	public void setFieldValue(Object obj, String fieldname, Object value) {
		Field field = null;
		try {
			field = getField(obj, fieldname);
			if (field != null) {
				if (field.getType() == Boolean.class) {
					value = (value.equals("1") || String.valueOf(value).equalsIgnoreCase("true")) ? Boolean.TRUE
							: Boolean.FALSE;
				}
				if (Modifier.isPublic(field.getModifiers())) {
					field.set(obj, value);
				} else {
					setFieldValueWithAccessor(obj, field, value);
				}
			}
		} catch (IllegalAccessException e1) {
			setFieldValueWithAccessor(obj, field, value);
		} catch (Exception e) {
			mxObjectCodec.log.log(Level.SEVERE,
					"Failed to set value \"" + value + "\" to field " + fieldname + " in " + obj, e);
		}
	}

	/**
	* Returns the template instance for the given field. This returns the value of the field, null if the value is an array or an empty collection if the value is a collection. The value is then used to populate the field for a new instance. For strongly typed languages it may be required to override this to return the correct collection instance based on the encoded child.
	*/
	public Object getFieldTemplate(Object obj, String fieldname, Node child) {
		Object template = getFieldValue(obj, fieldname);
		if (template != null && template.getClass().isArray()) {
			template = null;
		} else if (template instanceof Collection) {
			((Collection<?>) template).clear();
		}
		return template;
	}
}
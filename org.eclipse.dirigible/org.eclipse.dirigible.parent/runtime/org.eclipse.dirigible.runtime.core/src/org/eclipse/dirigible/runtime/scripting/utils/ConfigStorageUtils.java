/*******************************************************************************
 * Copyright (c) 2015 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 *******************************************************************************/

package org.eclipse.dirigible.runtime.scripting.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.dirigible.repository.ext.db.DBUtils;
import org.eclipse.dirigible.repository.logging.Logger;
import org.eclipse.dirigible.runtime.scripting.AbstractStorageUtils;
import org.eclipse.dirigible.runtime.scripting.EStorageException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConfigStorageUtils extends AbstractStorageUtils {

	private static final Gson GSON = new Gson();

	public static final String NO_PROPERTY_FOUND_ON_PATH = "No property found on path ";

	private static final String PUT_INSTANCE_OF_PROPERTIES_CLASS = "Put instance of Properties class";

	private static final Logger logger = Logger.getLogger(ConfigStorageUtils.class);

	private static final String DGB_CONFIG_STORAGE = "DGB_CONFIG_STORAGE";
	private static final String CONFIG_STORAGE_PATH = "CONFIG_STORAGE_PATH";
	private static final String CONFIG_STORAGE_DATA = "CONFIG_STORAGE_DATA";
	private static final String CONFIG_STORAGE_TIMESTAMP = "CONFIG_STORAGE_TIMESTAMP";

	private static final String INSERT_INTO_DGB_CONFIG_STORAGE = "INSERT INTO " + DGB_CONFIG_STORAGE + " (" + CONFIG_STORAGE_PATH + ", "
			+ CONFIG_STORAGE_DATA + ", " + CONFIG_STORAGE_TIMESTAMP + ")" + "VALUES (?,?,?)";

	// private static final String UPDATE_DGB_CONFIG_STORAGE = "UPDATE " + DGB_CONFIG_STORAGE + " SET " +
	// CONFIG_STORAGE_PATH + " = ?, "
	// + CONFIG_STORAGE_DATA + " = ?, " + CONFIG_STORAGE_TIMESTAMP + " = ?";

	private static final String DELETE_DGB_CONFIG_STORAGE = "DELETE FROM " + DGB_CONFIG_STORAGE;

	private static final String DELETE_DGB_STORAGE_PATH = "DELETE FROM " + DGB_CONFIG_STORAGE + " WHERE " + CONFIG_STORAGE_PATH + " = ?";

	private static final String CREATE_TABLE_DGB_CONFIG_STORAGE = "CREATE TABLE " + DGB_CONFIG_STORAGE + " (" + CONFIG_STORAGE_PATH
			+ " $KEY_VARCHAR$ PRIMARY KEY, " + CONFIG_STORAGE_DATA + " $BLOB$, " + CONFIG_STORAGE_TIMESTAMP + " TIMESTAMP" + " )";

	private static final String SELECT_COUNT_FROM_DGB_CONFIG_STORAGE = "SELECT COUNT(*) FROM " + DGB_CONFIG_STORAGE;

	private static final String SELECT_DGB_CONFIG_STORAGE = "SELECT * FROM " + DGB_CONFIG_STORAGE + " WHERE " + CONFIG_STORAGE_PATH + " = ?";

	private static final String SELECT_DGB_CONFIG_STORAGE_EXISTS = "SELECT " + CONFIG_STORAGE_PATH + " FROM " + DGB_CONFIG_STORAGE + " WHERE "
			+ CONFIG_STORAGE_PATH + " = ?";

	public ConfigStorageUtils(DataSource dataSource) {
		super(dataSource);
	}

	private void checkDB() throws NamingException, SQLException {
		super.checkDB(SELECT_COUNT_FROM_DGB_CONFIG_STORAGE, CREATE_TABLE_DGB_CONFIG_STORAGE);
	}

	@Override
	public boolean exists(String path) throws EStorageException {
		try {
			return super.exists(path, SELECT_DGB_CONFIG_STORAGE_EXISTS, SELECT_COUNT_FROM_DGB_CONFIG_STORAGE, CREATE_TABLE_DGB_CONFIG_STORAGE);
		} catch (SQLException e) {
			throw new EStorageException(e);
		}
	}

	@Override
	public void clear() throws EStorageException {
		try {
			super.clear(DELETE_DGB_CONFIG_STORAGE, SELECT_COUNT_FROM_DGB_CONFIG_STORAGE, CREATE_TABLE_DGB_CONFIG_STORAGE);
		} catch (SQLException e) {
			throw new EStorageException(e);
		}
	}

	@Override
	public void delete(String path) throws EStorageException {
		try {
			super.delete(path, DELETE_DGB_STORAGE_PATH, SELECT_COUNT_FROM_DGB_CONFIG_STORAGE, CREATE_TABLE_DGB_CONFIG_STORAGE);
		} catch (SQLException e) {
			throw new EStorageException(e);
		}
	}

	public void putProperty(String path, Object key, Object value) throws EStorageException {
		try {
			Properties properties = getProperties(path);
			if (properties == null) {
				properties = new Properties();
			}
			properties.put(key, value);
			putProperties(path, properties);
		} catch (Exception e) {
			throw new EStorageException(e);
		}
	}

	public void putProperties(String path, Properties properties) throws EStorageException {
		byte[] data = checkMaxSize(getByteArray(properties));

		try {
			checkDB();

			if (exists(path)) {
				delete(path);
				insert(path, data);
				// update(path, data);
			} else {
				insert(path, data);
			}

		} catch (Exception e) {
			throw new EStorageException(e);
		}
	}

	private byte[] checkMaxSize(byte[] data) {
		if (data.length > MAX_STORAGE_FILE_SIZE_IN_BYTES) {
			logger.warn(TOO_BIG_DATA_MESSAGE);
			throw new InvalidParameterException(TOO_BIG_DATA_MESSAGE);
		}
		return data;
	}

	private byte[] getByteArray(Properties properties) {
		byte[] data = new byte[] {};
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			properties.store(bos, "");
			data = bos.toByteArray();
			bos.close();
		} catch (IOException e) {
			logger.warn(PUT_INSTANCE_OF_PROPERTIES_CLASS);
			throw new InvalidParameterException(PUT_INSTANCE_OF_PROPERTIES_CLASS);
		}
		return data;
	}

	private void insert(String path, byte[] data) throws SQLException {
		DataSource dataSource = this.dataSource;
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			PreparedStatement pstmt = connection.prepareStatement(INSERT_INTO_DGB_CONFIG_STORAGE);

			int i = 0;
			pstmt.setString(++i, path);
			pstmt.setBinaryStream(++i, new ByteArrayInputStream(data), data.length);
			pstmt.setTimestamp(++i, new Timestamp(GregorianCalendar.getInstance().getTime().getTime()));

			pstmt.executeUpdate();

		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	// private void update(String path, byte[] data) throws SQLException {
	// DataSource dataSource = this.dataSource;
	// Connection connection = null;
	// try {
	// connection = dataSource.getConnection();
	// PreparedStatement pstmt = connection.prepareStatement(UPDATE_DGB_CONFIG_STORAGE);
	//
	// int i = 0;
	// pstmt.setString(++i, path);
	// pstmt.setBinaryStream(++i, new ByteArrayInputStream(data), data.length);
	// pstmt.setTimestamp(++i, new Timestamp(GregorianCalendar.getInstance().getTime().getTime()));
	//
	// pstmt.executeUpdate();
	//
	// } finally {
	// if (connection != null) {
	// connection.close();
	// }
	// }
	// }

	public Object getProperty(String path, Object key) throws EStorageException {
		Properties properties = getProperties(path);
		if (properties == null) {
			throw new InvalidParameterException(NO_PROPERTY_FOUND_ON_PATH + path);
		}
		return properties.get(key);
	}

	public Properties getProperties(String path) throws EStorageException {
		try {
			checkDB();

			DataSource dataSource = this.dataSource;
			Connection connection = null;
			try {
				connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement(SELECT_DGB_CONFIG_STORAGE);
				pstmt.setString(1, path);

				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					byte[] data = DBUtils.dbToDataBinary(connection, rs, CONFIG_STORAGE_DATA);
					ByteArrayInputStream bis = new ByteArrayInputStream(data);

					Properties properties = new Properties();
					properties.load(bis);

					bis.close();
					return properties;
				}

			} finally {
				if (connection != null) {
					connection.close();
				}
			}
		} catch (Exception e) {
			throw new EStorageException(e);
		}
		return null;
	}

	@Override
	public void put(String path, byte[] data) throws EStorageException {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			Properties properties = new Properties();
			properties.load(bis);
			putProperties(path, properties);
		} catch (IOException e) {
			throw new EStorageException(e);
		}

	}

	public void putJson(String path, String json) throws EStorageException {
		try {
			Properties properties = new Properties();
			JsonElement jsonElement = new JsonParser().parse(json);
			JsonObject jsonData = jsonElement.getAsJsonObject();
			Iterator<Entry<String, JsonElement>> entries = jsonData.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String, JsonElement> entry = entries.next();
				String key = new String(entry.getKey());
				String value = new String(entry.getValue().getAsString());
				properties.setProperty(key, value);
			}
			putProperties(path, properties);
		} catch (Exception e) {
			throw new EStorageException(e);
		}
	}

	public String getJson(String path) throws EStorageException {
		try {
			JsonObject jsonObject = new JsonObject();
			Properties properties = getProperties(path);
			if (properties != null) {
				Enumeration keys = properties.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement().toString();
					String value = properties.getProperty(key);
					jsonObject.addProperty(key, value);
				}
				return jsonObject.toString();
			}
		} catch (Exception e) {
			throw new EStorageException(e);
		}
		return null;
	}

	@Override
	public void put(String path, byte[] data, String contentType) throws EStorageException {
		put(path, data);
	}

	@Override
	public byte[] get(String path) throws EStorageException {
		try {
			Properties properties = getProperties(path);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			properties.store(baos, "");
			return baos.toByteArray();
		} catch (IOException e) {
			throw new EStorageException(e);
		}
	}

}
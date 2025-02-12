package com.ibm.iotf.client.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.IoTFCReSTException;
import com.ibm.iotf.util.LoggerUtility;

/**
 * Class to register, delete and retrieve information about devices <br>
 * This class can also be used to retrieve historian information
 */

public class APIClient {

	private static final String CLASS_NAME = APIClient.class.getName();
	
	private static final String BASIC_API_V0002_URL = "internetofthings.ibmcloud.com/api/v0002";

	private String authKey = null;
	private String authToken = null;
	private SSLContext sslContext = null;
	private String orgId = null;
	public APIClient(Properties opt) throws NoSuchAlgorithmException, KeyManagementException {
		boolean isGateway = false;
		String authKeyPassed = null;
		if("gateway".equals(getAuthMethod(opt))) {
			isGateway = true;
		} else {
			authKeyPassed = opt.getProperty("auth-key");
			if(authKeyPassed == null) {
				authKeyPassed = opt.getProperty("API-Key");
			}
		
			authKey = trimedValue(authKeyPassed);
		}
		
		String token = opt.getProperty("auth-token");
		if(token == null) {
			token = opt.getProperty("Authentication-Token");
		}
		authToken = trimedValue(token);

		String org = null;
		org = opt.getProperty("org");
		
		if(org == null) {
			org = opt.getProperty("Organization-ID");
		}
		
		this.orgId = trimedValue(org);
		
		if(isGateway) {
			authKey = "g-" + this.orgId + '-' + this.getGWDeviceType(opt) + '-' + this.getGWDeviceId(opt);
		}

		sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, null, null);
	}
	
	private static String getAuthMethod(Properties opt) {
		String method = opt.getProperty("auth-method");
		if(method == null) {
			method = opt.getProperty("Authentication-Method");
		}
		
		return trimedValue(method);
	}
	
	/*
	 * old style - id
	 * new style - Device-ID
	 */
	protected String getGWDeviceId(Properties options) {
		String id = null;
		id = options.getProperty("Gateway-ID");
		if(id == null) {
			id = options.getProperty("Device-ID");
		}
		if(id == null) {
			id = options.getProperty("id");
		}
		return trimedValue(id);
	}
	
	protected String getGWDeviceType(Properties options) {
		String type = null;
		type = options.getProperty("Gateway-Type");
		if(type == null) {
			type = options.getProperty("Device-Type");
		}
		if(type == null) {
			type = options.getProperty("type");
		}
		return trimedValue(type);
	}
	
	private static String trimedValue(String value) {
		if(value != null) {
			return value.trim();
		}
		return value;
	}
	
	private HttpResponse connect(String httpOperation, String url, String jsonPacket, 
			ArrayList<NameValuePair> queryParameters) throws IoTFCReSTException, URISyntaxException, IOException {
		final String METHOD = "connect";
		
		StringEntity input = null;
		try {
			if(jsonPacket != null) {
				input = new StringEntity(jsonPacket);
			}
		} catch (UnsupportedEncodingException e) {
			LoggerUtility.warn(CLASS_NAME, METHOD, "Unable to carry out the ReST request");
			throw e;
		}
		
		byte[] encoding = Base64.encodeBase64(new String(authKey + ":" + authToken).getBytes() );			
		String encodedString = new String(encoding);
		switch(httpOperation) {
			case "post":
				URIBuilder builder = new URIBuilder(url);
				if(queryParameters != null) {
					builder.setParameters(queryParameters);
				}

				HttpPost post = new HttpPost(builder.build());
				post.setEntity(input);
				post.addHeader("Content-Type", "application/json");
				post.addHeader("Accept", "application/json");
				post.addHeader("Authorization", "Basic " + encodedString);
				try {
					HttpClient client = HttpClientBuilder.create().setSslcontext(sslContext).build();
					HttpResponse response = client.execute(post);
					return response;
				} catch (IOException e) {
					LoggerUtility.warn(CLASS_NAME, METHOD, e.getMessage());
					throw e;
				} finally {

				}

			case "put":
				URIBuilder putBuilder = new URIBuilder(url);
				if(queryParameters != null) {
					putBuilder.setParameters(queryParameters);
				}
				HttpPut put = new HttpPut(putBuilder.build());
				put.setEntity(input);
				put.addHeader("Content-Type", "application/json");
				put.addHeader("Accept", "application/json");
				put.addHeader("Authorization", "Basic " + encodedString);
				try {
					HttpClient client = HttpClientBuilder.create().setSslcontext(sslContext).build();
					HttpResponse response = client.execute(put);
					return response;
				} catch (IOException e) {
					LoggerUtility.warn(CLASS_NAME, METHOD, e.getMessage());
					throw e;
				} finally {

				}
				
			case "get":

				URIBuilder getBuilder = new URIBuilder(url);
				if(queryParameters != null) {
					getBuilder.setParameters(queryParameters);
				}
				HttpGet get = new HttpGet(getBuilder.build());
				get.addHeader("Content-Type", "application/json");
				get.addHeader("Accept", "application/json");
				get.addHeader("Authorization", "Basic " + encodedString);
				try {
					HttpClient client = HttpClientBuilder.create().setSslcontext(sslContext).build();					
					HttpResponse response = client.execute(get);
					return response;
				} catch (IOException e) {
					LoggerUtility.warn(CLASS_NAME, METHOD, e.getMessage());
					throw e;
				}			

			case "delete":
				URIBuilder deleteBuilder = new URIBuilder(url);
				if(queryParameters != null) {
					deleteBuilder.setParameters(queryParameters);
				}

				HttpDelete delete = new HttpDelete(deleteBuilder.build());
				delete.addHeader("Content-Type", "application/json");
				delete.addHeader("Accept", "application/json");
				delete.addHeader("Authorization", "Basic " + encodedString);
				try {
					HttpClient client = HttpClientBuilder.create().setSslcontext(sslContext).build();					
					return client.execute(delete);
				} catch (IOException e) {
					LoggerUtility.warn(CLASS_NAME, METHOD, e.getMessage());
					throw e;
				} finally {

				}
		}
		return null;
			
	}
	
	private String readContent(HttpResponse response, String method) 
			throws IllegalStateException, IOException {
		
		BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			LoggerUtility.warn(CLASS_NAME, method, e.getMessage());
			throw e;
		}
		LoggerUtility.fine(CLASS_NAME, method, line);
		try {
			if(br != null)
				br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}
	
	/**
	 * Checks whether the given device exists in the Watson IoT Platform
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/get_device_types_typeId">link</a>
	 * for more information about the response</p>.
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return A boolean response containing the status
	 * @throws IoTFCReSTException
	 */
	public boolean isDeviceExist(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "isDeviceExist";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the Device "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			return false;
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}

	/**
	 * This method retrieves a device based on the deviceType and DeviceID of the organization passed.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices_deviceId">link</a>
	 * for more information about the response.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonObject
	 * @throws IOException 
	 */
	public JsonObject getDevice(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "getDevice";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the Device "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The device type does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Gets location information for a device.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices_deviceId_location">link</a>
	 * for more information about the response.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonObject
	 * @throws IOException 
	 */
	public JsonObject getDeviceLocation(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "getDeviceLocation";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/location");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieveing the Device Location "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 404) {
			throw new IoTFCReSTException(code, "Device location information not found");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Updates the location information for a device. If no date is supplied, the entry is added with the current date and time.
	 *  
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * @param location contains the new location
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/put_device_types_typeId_devices_deviceId_location">link</a>
	 * for more information about the JSON format</p>.
	 *   
	 * @return A JSON response containing the status of the update operation.
	 * 
	 * @throws IoTFCReSTException
	 */
	public JsonObject updateDeviceLocation(String deviceType, String deviceId, 
			JsonElement location) throws IoTFCReSTException {
		final String METHOD = "updateDeviceLocation";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/location");
		
		int code = 0;
		JsonElement jsonResponse = null;
		HttpResponse response = null;
		try {
			response = connect("put", sb.toString(), location.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200 || code == 409) {
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
				if(code == 200) {
					return jsonResponse.getAsJsonObject();
				}
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in updating the Device Location "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 404) {
			throw new IoTFCReSTException(code, "Device location information not found");
		} else if(code == 409) {
			throw new IoTFCReSTException(code, "The update could not be completed due to a conflict", jsonResponse);
		} else if (code == 500) {		
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Gets device management information for a device.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices_deviceId_mgmt">link</a>
	 * for more information about the JSON Response.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonObject
	 * @throws IOException 
	 */
	public JsonObject getDeviceManagementInformation(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "getDeviceManagementInformation";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/mgmt");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieveing the Device Management Information "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 404) {
			throw new IoTFCReSTException(code, "Device not found");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Gets device type details.
	 *  
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * @param propertiesToBeModified contains the parameters to be updated
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/put_device_types_typeId_devices_deviceId">link</a>
	 * for more information about the response</p>.
	 *   
	 * @return A JSON response containing the status of the update operation.
	 * 
	 * @throws IoTFCReSTException
	 */
	public JsonObject updateDevice(String deviceType, String deviceId, 
			JsonElement propertiesToBeModified) throws IoTFCReSTException {
		
		final String METHOD = "updateDevice";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId);
		
		int code = 0;
		JsonElement jsonResponse = null;
		HttpResponse response = null;
		try {
			response = connect("put", sb.toString(), propertiesToBeModified.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200 || code == 409) {
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
				if(code == 200) {
					return jsonResponse.getAsJsonObject();
				}
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in updating the Device "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The organization, device type or device does not exist");
		} else if(code == 409) {
			throw new IoTFCReSTException(code, "The update could not be completed due to a conflict", jsonResponse);
		} else if (code == 500) {		
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}


	/**
	 * Get details about an organization. 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Organization_Configuration/get">link</a>
	 * for more information about the response.</p>
	 *   
	 * @return details about an organization.
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getOrganizationDetails() throws IoTFCReSTException {
		final String METHOD = "getOrganizationDetails";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 * http://iot-test-01.hursley.ibm.com/docs/api/v0002.html#!/Organization_Configuration/get
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				// success
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the Organization detail, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the api key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The organization does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}

	/**
	 * This method returns all the devices belonging to the organization, This method
	 * provides more control in returning the response over the no argument method.
	 * 
	 * <p>For example, Sorting can be performed on any of the properties.</p>
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Bulk_Operations/get_bulk_devices">link</a>
	 * for more information about how to control the response.</p>
	 * 
	 * @param parameters list of query parameters that controls the output. For more information about the
	 * list of possible query parameters, refer to this 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Bulk_Operations/get_bulk_devices">link</a>. 
	 *   
	 * @return JSON response containing the list of devices.
	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of devices.</p>
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getAllDevices(ArrayList<NameValuePair> parameters) throws IoTFCReSTException {
		final String METHOD = "getDevices(1)";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/bulk/devices");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				// success
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the Device details, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the api key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The organization does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * This method returns all the devices belonging to the organization
	 * 
	 * <p> Invoke the overloaded method, if you want to have control over the response, for example sorting.</p>
	 * 
	 * @return Jsonresponse containing the list of devices. Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Bulk_Operations/get_bulk_devices">link</a>
	 * for more information about the response.
	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of devices.</p>
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getAllDevices() throws IoTFCReSTException {
		return getAllDevices((ArrayList<NameValuePair>)null);
	}
	
	/**
	 * This method returns all the devices belonging to a particular device type, This method
	 * provides more control in returning the response over the no argument method.
	 * 
	 * <p>For example, Sorting can be performed on any of the properties.</p>
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices">link</a>
	 * for more information about how to control the response.</p>
	 * 
	 * @param deviceType Device type ID
	 * @param parameters list of query parameters that controls the output. For more information about the
	 * list of possible query parameters, refer to this 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices">link</a>.
	 * 
	 * @return JSON response containing the list of devices.
	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of devices.</p>
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject retrieveDevices(String deviceType, ArrayList<NameValuePair> parameters) throws IoTFCReSTException {
		
		final String METHOD = "getDevices(typeID)";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).append("/devices");
				   
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				// success
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the Device details, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the api key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The organization does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * This method returns all the devices belonging to a particular device type in an organization.
	 * 
	 * <p> Invoke the overloaded method, if you want to have control over the response, for example sorting.</p>
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices">link</a>
	 * for more information about how to control the response.</p>
	 * 
	 * @param deviceType Device type ID 
	 * 
	 * @return JSON response containing the list of devices.
	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of devices.</p>
	 * 	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject retrieveDevices(String deviceType) throws IoTFCReSTException {
		return retrieveDevices(deviceType, (ArrayList)null);
	}
	
	/**
	 * This method returns all devices that are connected through the specified gateway(typeId, deviceId) to Watson IoT Platform.
	 * 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/get_device_types_typeId_devices_deviceId_devices">link</a>
	 * for more information about how to control the response.</p>
	 * 
	 * @param gatewayType Gateway Device type ID 
	 * @param gatewayId Gateway Device ID
	 * 
	 * @return JSON response containing the list of devices.
	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of devices.</p>
	 * 	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDevicesConnectedThroughGateway(String gatewayType, String gatewayId) throws IoTFCReSTException {
		final String METHOD = "getDevicesConnectedThroughGateway(typeID, deviceId)";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(gatewayType).
		   append("/devices").
		   append(gatewayId).append("/devices");
				   
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				// success
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the device information "
					+ "that are connected through the specified gateway, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if(code == 403) {
			throw new IoTFCReSTException(code, "Request is only allowed if the classId of the device type is 'Gateway'");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "Device type or device not found");
		} else if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}



	/**
	 * This method returns all the device types belonging to the organization, This method
	 * provides more control in returning the response over the no argument method.
	 * 
	 * <p>For example, Sorting can be performed on any of the properties.</p>
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/get_device_types">link</a>
	 * for more information about how to control the response.</p>
	 * 
	 * @param parameters list of query parameters that controls the output. For more information about the
	 * list of possible query parameters, refer to this 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/get_device_types">link</a>.
	 * 
	 * @return A JSON response containing the list of device types.
	 * 	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of device types.</p>
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getAllDeviceTypes(ArrayList<NameValuePair> parameters) throws IoTFCReSTException {
		final String METHOD = "getDeviceTypes";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 * http://iot-test-01.hursley.ibm.com/docs/api/v0002.html#!/Bulk_Operations/get_bulk_devices
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types");
		
		HttpResponse response = null;
		int code = 0;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				// success
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the DeviceType details, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the api key used does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * This method returns all the device types belonging to the organization. 
	 * <p> Invoke the overloaded method, if you want to have control over the response, for example sorting.</p>
	 * 
	 * @return A JSON response containing the list of device types. Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/get_device_types">link</a>
	 * for more information about the response.
	 * <p> The response will contain more parameters that can be used to issue the next request. 
	 * The result element will contain the current list of device types.</p>
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDeviceTypes() throws IoTFCReSTException {
		return getAllDeviceTypes(null);
	}
	
	
	/**
	 * Check whether the given device type exists in the Watson IoT Platform
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/get_device_types_typeId">link</a>
	 * for more information about the response</p>.
	 * @param deviceType The device type to be checked in Watson IoT Platform
	 * @return A boolean response containing the status
	 * @throws IoTFCReSTException
	 */
	public boolean isDeviceTypeExist(String deviceType) throws IoTFCReSTException {
		final String METHOD = "isDeviceTypeExist";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the Device Type "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			return false;
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}
	
	/**
	 * Gets device type details.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/get_device_types_typeId">link</a>
	 * for more information about the response</p>.
	 *   
	 * @return A JSON response containing the device type.
	 * 
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDeviceType(String deviceType) throws IoTFCReSTException {
		final String METHOD = "getDeviceType";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the Device Type "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The device type does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Updates device type details.
	 * 
	 * @param updatedValues contains the parameters to be updated
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/put_device_types_typeId">link</a>
	 * for more information about the response</p>.
	 *   
	 * @return A JSON response containing the status of the update operation.
	 * 
	 * @throws IoTFCReSTException 
	 */
	public JsonObject updateDeviceType(String deviceType, JsonElement updatedValues) throws IoTFCReSTException {
		final String METHOD = "updateDeviceType";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType);
		
		int code = 0;
		JsonElement jsonResponse = null;
		HttpResponse response = null;
		try {
			response = connect("put", sb.toString(), updatedValues.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200 || code == 409) {
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
				if(code == 200) {
					return jsonResponse.getAsJsonObject();
				}
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in updating the Device Type "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The device type does not exist");
		} else if(code == 409) {
			throw new IoTFCReSTException(code, "The update could not be completed due to a conflict", jsonResponse);
		} else if (code == 500) {		
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	
	/**
	 * Creates a device type.
	 * 
	 * @param deviceType JSON object representing the device type to be added. Refer to  
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/post_device_types">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @return JSON object containing the response of device type.
	 *  
	 * @throws IoTFCReSTException 
	 */

	public JsonObject addDeviceType(JsonElement deviceType) throws IoTFCReSTException {
		
		final String METHOD = "addDeviceType";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types");
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("post", sb.toString(), deviceType.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 201 || code == 400 || code == 409) {
				// success
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
			}
			if(code == 201) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in adding the device Type "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 400) {
			throw new IoTFCReSTException(400, "Invalid request (No body, invalid JSON, "
					+ "unexpected key, bad value)", jsonResponse);
		} else if(code == 401) {
			throw new IoTFCReSTException(401, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(403, "The authentication method is invalid or "
					+ "the API key used does not exist");
		} else if (code == 409) {
			throw new IoTFCReSTException(409, "The device type already exists", jsonResponse);  
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Creates a gateway device type.
	 * 
	 * @param deviceType JSON object representing the gateway device type to be added. Refer to  
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/post_device_types">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @return JSON object containing the response of device type.
	 *  
	 * @throws IoTFCReSTException 
	 */

	public JsonObject addGatewayDeviceType(JsonElement deviceType) throws IoTFCReSTException {
		
		if(deviceType != null && !deviceType.getAsJsonObject().has("classId")) {
			deviceType.getAsJsonObject().addProperty("classId", "Gateway");
		}
		return this.addDeviceType(deviceType);
	}


	/**
	 * 
	 * Creates a device type.Refer to  
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/post_device_types">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @param id ID of the Device Type to be added
	 * @param description Description of the device Type to be added
	 * @param deviceInfo DeviceInfo to be added. Must be specified in JSON format
	 * @param metadata Metadata to be added
	 * 
	 * @return JSON object containing the response of device type.
	 * 
	 * @throws IoTFCReSTException
	 */

	public JsonObject addDeviceType(String id, String description, 
			JsonElement deviceInfo, JsonElement metadata) throws IoTFCReSTException {
		
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types");

		JsonObject input = new JsonObject();
		if(id != null) {
			input.addProperty("id", id);
		}
		if(description != null) {
			input.addProperty("description", description);
		}
		if(deviceInfo != null) {
			input.add("deviceInfo", deviceInfo);
		}
		if(metadata != null) {
			input.add("metadata", metadata);
		}
		
		return this.addDeviceType(input);
	}
	
	/**
	 * Deletes a device type.
	 * 
	 * @param typeId DeviceType to be deleted from IBM Watson IoT Platform
	 *   
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Types/delete_device_types_typeId">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @return JSON object containing the response of device type.
	 *  
	 * @throws IoTFCReSTException 
	 */

	public boolean deleteDeviceType(String typeId) throws IoTFCReSTException {
		final String METHOD = "deleteDeviceType";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(typeId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("delete", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 204) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Device Type "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}
	
	/**
	 * This method retrieves events across all devices registered in the organization. 
	 * Use the overloaded method to control the output.
	 *  
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents() throws IoTFCReSTException {		
		return getHistoricalEvents(null, null, null);
	}
	
	/**
	 * This method retrieves events across all devices registered in the organization. 
	 * Use the overloaded method to control the output.
	 *  
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param parameters Contains the list of query parameters as specified in the 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian">link</a>
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(ArrayList<NameValuePair> parameters) throws IoTFCReSTException {		
		return getHistoricalEvents(null, null, parameters);
	}
	
	/**
	 * This method retrieves events across all devices of a particular device type. 
	 * Use the overloaded method to control the output.
	 *  
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(String deviceType) throws IoTFCReSTException {		
		return getHistoricalEvents(deviceType, null, null);
	}
	
	/**
	 * This method retrieves events across all devices of a particular device type but with a
	 * list of query parameters,
	 *  
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param parameters Contains the list of query parameters as specified in the 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType">link</a>
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(String deviceType, 
			ArrayList<NameValuePair> parameters) throws IoTFCReSTException {		
		return getHistoricalEvents(deviceType, null, parameters);
	}

	/**
	 * This method retrieves events based on the device ID
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType_devices_deviceId">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(String deviceType, String deviceId) throws IoTFCReSTException {
		return getHistoricalEvents(deviceType, deviceId, null);
	}
	
	/**
	 * This method retrieves events based on the device ID
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType_devices_deviceId">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * @param parameters Contains the list of query parameters as specified in the 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType_devices_deviceId">link</a>
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(String deviceType, 
			String deviceId, ArrayList<NameValuePair> parameters) throws IoTFCReSTException {
		final String METHOD = "getHistoricalEvents(3)";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/historian");
		
		if(deviceType != null) {
			sb.append("/types/").append(deviceType);
		}
		
		if(deviceId != null) {
			sb.append("/devices/").append(deviceId);
		}
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			JsonElement jsonResponse = new JsonParser().parse(result);
			return jsonResponse;
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException(code, "Failure in retrieving "
					+ "the Historical events. :: "+e.getMessage());
			ex.initCause(e);
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * This method retrieves events based on the device ID
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType_devices_deviceId">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @param bookmark(can be empty or null) Used for paging through results. Issue the first request without specifying a bookmark, 
	 * then take the bookmark returned in the response and provide it on the request for the next page. 
	 * Repeat until the end of the result set indicated by the absence of a bookmark. 
	 * Each request must use exactly the same values for the other parameters, or the results are undefined.
	 * 
	 * @param evtType (can be empty or null) Restrict results only to those events published under this event identifier.
	 * @param start(can be empty or null) Number of milliseconds since January 1, 1970, 00:00:00 GMT). Restrict results to events published after this date
	 * @param end(can be empty or null)  Number of milliseconds since January 1, 1970, 00:00:00 GMT). Restrict results to events published before this date
	 * 
	 * @return JsonArray
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(String deviceType, 
			String deviceId, 
			String bookmark,
			String evtType, 
			String start, 
			String end) throws IoTFCReSTException {
		
		return getHistoricalEvents(deviceType, deviceId, bookmark, evtType, start, end, -1, null, null);
	}
	
	/**
	 * This method retrieves events based on the device ID
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Historical_Event_Retrieval/get_historian_types_deviceType_devices_deviceId">link</a>
	 * for more information about the query parameters and response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @param bookmark(can be empty or null) Used for paging through results. Issue the first request without specifying a bookmark, 
	 * then take the bookmark returned in the response and provide it on the request for the next page. 
	 * Repeat until the end of the result set indicated by the absence of a bookmark. 
	 * Each request must use exactly the same values for the other parameters, or the results are undefined.
	 * 
	 * @param evtType (can be empty or null) Restrict results only to those events published under this event identifier.
	 * @param start(can be empty or null) Number of milliseconds since January 1, 1970, 00:00:00 GMT). Restrict results to events published after this date
	 * @param end(can be empty or null)  Number of milliseconds since January 1, 1970, 00:00:00 GMT). Restrict results to events published before this date
	 * @param top Number between 1 and 100. Restrict the number of records returned (default=100)
	 * @param summarize Array. A list of fields from the JSON event payload on which to perform the aggregate function specified by the summarize_type parameter. 
	 * The format for the parameter is {field1,field2,...,fieldN}
	 * 
	 * @param summarizeType The aggregation to perform on the fields specified by the summarize parameter.
	 * @return
	 * @throws IoTFCReSTException
	 */
	public JsonElement getHistoricalEvents(String deviceType, 
			String deviceId, 
			String bookmark,
			String evtType, 
			String start, 
			String end,
			int top,
			String summarize,
			String summarizeType) throws IoTFCReSTException {
		
		final String METHOD = "getHistoricalEvents(9)";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/historian/");
		
		if(deviceType != null) {
			sb.append("types/").append(deviceType);
		}
		
		if(deviceId != null) {
			sb.append("/devices/").append(deviceId);
		}
		
		/**
		 * Create the query parameters based on the swagger UI
		 */
		
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		if(bookmark != null) {
			parameters.add(new BasicNameValuePair("_bookmark", bookmark));
		}
		if(evtType != null) {
			parameters.add(new BasicNameValuePair("evt_type", evtType));
		}
		if(start != null) {
			parameters.add(new BasicNameValuePair("start", start));
		}
		if(end != null) {
			parameters.add(new BasicNameValuePair("end", end));
		}
		if(top >=0 && top <= 100) {
			parameters.add(new BasicNameValuePair("top", Integer.toString(top)));
		}
		if(summarize != null) {
			parameters.add(new BasicNameValuePair("summarize", summarize));
		}
		if(summarizeType != null) {
			parameters.add(new BasicNameValuePair("summarize_type", summarizeType));
		}
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			JsonElement jsonResponse = new JsonParser().parse(result);
			return jsonResponse;
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException(code, "Failure in retrieving "
					+ "the Historical events. :: "+e.getMessage());
			ex.initCause(e);
		}
		throwException(response, METHOD);
		return null;
	}


	/**
	 * This method registers a device, by accepting more parameters. Refer to 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/post_device_types_typeId_devices">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @param deviceType String representing device type.
	 * @param deviceId String representing device id.
	 * @param authToken String representing the authentication token of the device (can be null). If its null
	 * the IBM Watson IoT Platform will generate a token.
	 * @param deviceInfo JsonObject representing the device Info (can be null).
	 * @param location JsonObject representing the location of the device (can be null).
	 * @param metadata JsonObject representing the device metadata (can be null).
	 * 
	 * @return JsonObject containing the registered device details
	 * @throws IoTFCReSTException
	 */
	public JsonObject registerDevice(String deviceType, String deviceId, 
			String authToken, JsonElement deviceInfo, JsonElement location, 
			JsonElement metadata) throws IoTFCReSTException {
		
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices");
		
		JsonObject input = new JsonObject();
		if(deviceId != null) {
			input.addProperty("deviceId", deviceId);
		}
		if(authToken != null) {
			input.addProperty("authToken", authToken);
		}
		if(deviceInfo != null) {
			input.add("deviceInfo", deviceInfo);
		}
		if(location != null) {
			input.add("location", location);
		}
		if(metadata != null) {
			input.add("metadata", metadata);
		}

		return this.registerDevice(deviceType, input);
	}
	
	/**
	 * Register a new device.
	 *  
	 * The response body will contain the generated authentication token for the device. 
	 * The caller of the method must make sure to record the token when processing 
	 * the response. The IBM Watson IoT Platform will not be able to retrieve lost authentication tokens.
	 * 
	 * @param typeId DeviceType ID 
	 * 
	 * @param device JSON representation of the device to be added. Refer to 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/post_device_types_typeId_devices">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @return JsonObject containing the generated authentication token for the device. 
	 *  
	 * @throws IoTFCReSTException 
	 */

	public JsonObject registerDevice(String typeID, JsonElement device) throws IoTFCReSTException {
		final String METHOD = "registerDevice";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(typeID).
		   append("/devices");
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("post", sb.toString(), device.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 201 || code == 400 || code == 409) {
				// success
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
			}
			if(code == 201) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in registering the device "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 400) {
			throw new IoTFCReSTException(400, "Invalid request (No body, invalid JSON, "
					+ "unexpected key, bad value)", jsonResponse);
		} else if(code == 401) {
			throw new IoTFCReSTException(401, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(403, "The authentication method is invalid or "
					+ "the API key used does not exist");
		} else if (code == 409) {
			throw new IoTFCReSTException(409, "The device already exists", jsonResponse);  
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Register a new device under the given gateway.
	 *  
	 * The response body will contain the generated authentication token for the device. 
	 * The caller of the method must make sure to record the token when processing 
	 * the response. The IBM Watson IoT Platform will not be able to retrieve lost authentication tokens.
	 * 
	 * @param typeId DeviceType ID
	 * @param gatewayId The deviceId of the gateway 
	 * @param gatewayTypeId The device type of the gateway  
	 * 
	 * @param device JSON representation of the device to be added. Refer to 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/post_device_types_typeId_devices">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @return JsonObject containing the generated authentication token for the device. 
	 *  
	 * @throws IoTFCReSTException 
	 */

	public JsonObject registerDeviceUnderGateway(String typeID, String gatewayId, 
			String gatewayTypeId, JsonElement device) throws IoTFCReSTException {
		
		if(device != null) {
			JsonObject deviceObj = device.getAsJsonObject();
			deviceObj.addProperty("gatewayId", gatewayId);
			deviceObj.addProperty("gatewayTypeId", gatewayTypeId);
		}
		return this.registerDevice(typeID, device);
	}
	
	/**
	 * This method deletes the device which matches the device id and type of the organization.	 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Devices/delete_device_types_typeId_devices_deviceId">link</a>
	 * for more information about the response</p>.

	 * @param deviceType
	 * 				object of String which represents device Type
	 * @param deviceId
	 * 				object of String which represents device id
	 * @return boolean to denote success or failure of operation
	 * @throws IOException 
	 */
	public boolean deleteDevice(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "deleteDevice";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("delete", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 204) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Device"
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		
		throwException(response, METHOD);
		return false;
	}
	
	/**
	 * This method Clears the diagnostic log for the device.	 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/delete_device_types_typeId_devices_deviceId_diag_logs">link</a>
	 * for more information about the JSON message format</p>.

	 * @param deviceType
	 * 				object of String which represents device Type
	 * @param deviceId
	 * 				object of String which represents device id
	 * @return boolean to denote success or failure of operation
	 * @throws IOException 
	 */
	public boolean clearAllDiagnosticLogs(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "clearDiagnosticLogs";
		
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/logs");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("delete", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 204) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Diagnostic Logs "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		
		throwException(response, METHOD);
		return false;
	}
	
	/**
	 * This method retrieves all the diagnostic logs for a device.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/get_device_types_typeId_devices_deviceId_diag_logs">link</a>
	 * for more information about the response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonArray
	 * @throws IOException 
	 */
	public JsonArray getAllDiagnosticLogs(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "getAllDiagnosticLogs";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/logs");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonArray();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the diagnostic logs "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}		
		if(code == 404) {
			throw new IoTFCReSTException(code, "Device log not found");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}

	
	/**
	 * Adds an entry in the log of diagnostic information for the device. 
	 * The log may be pruned as the new entry is added. If no date is supplied, 
	 * the entry is added with the current date and time.
	 * 
	 * <p> Refer to  
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/post_device_types_typeId_devices_deviceId_diag_logs">link</a> 
	 * for more information about the schema to be used </p>
	 * 
 	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id

	 * @return boolean containing the status of the load addition.
	 *  
	 * @throws IoTFCReSTException 
	 */

	public boolean addDiagnosticLog(String deviceType, String deviceId, JsonElement log) throws IoTFCReSTException {
		final String METHOD = "addDiagnosticLog";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/logs");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("post", sb.toString(), log.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 201 ) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in adding the diagnostic Log "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}
	
	/**
	 * Delete this diagnostic log for the device.	 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/delete_device_types_typeId_devices_deviceId_diag_logs_logId">link</a>
	 * for more information about the JSON Format</p>.

	 * @param deviceType
	 * 				object of String which represents device Type
	 * @param deviceId
	 * 				object of String which represents device id
	 * 
	 * @param logId object of String which represents log id
	 *  
	 * @return boolean to denote success or failure of operation
	 * @throws IOException 
	 */
	public boolean deleteDiagnosticLog(String deviceType, String deviceId, String logId) throws IoTFCReSTException {
		final String METHOD = "deleteDiagnosticLog";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/logs/").
		   append(logId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("delete", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 204) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Diagnostic Log "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}
	
	private void throwException(HttpResponse response, String method) throws IoTFCReSTException {
		int code = response.getStatusLine().getStatusCode();
		JsonElement jsonResponse = null;
		try {
			String result = this.readContent(response, method);
			jsonResponse = new JsonParser().parse(result);
		} catch(Exception e) {}
		
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	
	/**
	 * Gets diagnostic log for a device.	 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/delete_device_types_typeId_devices_deviceId_diag_logs_logId">link</a>
	 * for more information about the JSON Format</p>.

	 * @param deviceType
	 * 				object of String which represents device Type
	 * @param deviceId
	 * 				object of String which represents device id
	 * 
	 * @param logId object of String which represents log id
	 *  
	 * @return JsonObject the DiagnosticLog in JSON Format
	 * 
	 * @throws IOException 
	 */
	public JsonObject getDiagnosticLog(String deviceType, String deviceId, String logId) throws IoTFCReSTException {
		final String METHOD = "getDiagnosticLog";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/logs/").
		   append(logId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the Diagnostic Log "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 404) {
			throw new IoTFCReSTException(code, "Device not found");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}


	/**
	 * Clears the list of error codes for the device. The list is replaced with a single error code of zero.	 
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/delete_device_types_typeId_devices_deviceId_diag_errorCodes">link</a>
	 * for more information about the JSON message format</p>.

	 * @param deviceType
	 * 				object of String which represents device Type
	 * @param deviceId
	 * 				object of String which represents device id
	 * @return boolean to denote success or failure of operation
	 * @throws IOException 
	 */
	public boolean clearAllDiagnosticErrorCodes(String deviceType, String deviceId) throws IoTFCReSTException {
		String METHOD = "clearAllDiagnosticErrorCodes";
		
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/errorCodes");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("delete", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 204) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Diagnostic Errorcodes "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}
	
	/**
	 * This method retrieves all the diagnostic error codes for a device.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/get_device_types_typeId_devices_deviceId_diag_errorCodes">link</a>
	 * for more information about the response in JSON format.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonArray
	 * @throws IOException 
	 */
	public JsonArray getAllDiagnosticErrorCodes(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "getAllDiagnosticErrorCodes";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/errorCodes");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonArray();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the diagnostic Errorcodes "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 404) {
			throw new IoTFCReSTException(code, "Device not found");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}

	/**
	 * Adds an error code to the list of error codes for the device. 
	 * The list may be pruned as the new entry is added.
	 * 
	 * <p> Refer to  
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/post_device_types_typeId_devices_deviceId_diag_errorCodes">link</a> 
	 * for more information about the schema to be used </p>
	 * 
 	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * @param errorcode ErrorCode to be added in Json Format
	 * 
	 * @return boolean containing the status of the add operation.
	 *  
	 * @throws IoTFCReSTException 
	 */

	public boolean addDiagnosticErrorCode(String deviceType, String deviceId, JsonElement errorcode) throws IoTFCReSTException {
		final String METHOD = "addDiagnosticErrorCode";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/device/types/").
		   append(deviceType).
		   append("/devices/").
		   append(deviceId).
		   append("/diag/errorCodes");
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("post", sb.toString(), errorcode.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 201) {
				return true;
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in adding the Errorcode "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return false;
	}

	/**
	 * Adds an error code to the list of error codes for the device. 
	 * The list may be pruned as the new entry is added.
	 * 
	 * <p> Refer to  
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Device_Diagnostics/post_device_types_typeId_devices_deviceId_diag_errorCodes">link</a> 
	 * for more information about the schema to be used </p>
	 * 
 	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * @param errorcode ErrorCode to be added in integer format
	 * @param date current date (can be null)
	 * 
	 * @return boolean containing the status of the add operation.
	 *  
	 * @throws IoTFCReSTException 
	 */

	public boolean addDiagnosticErrorCode(String deviceType, String deviceId, 
			int errorcode, Date date) throws IoTFCReSTException {
		
		JsonObject ec = new JsonObject();
		ec.addProperty("errorCode", errorcode);
		if(date == null) {
			date = new Date();
		}
		String utcTime = DateFormatUtils.formatUTC(date, 
				DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern());
		
		ec.addProperty("timestamp", utcTime);
		return addDiagnosticErrorCode(deviceType, deviceId, ec);
	}
	
	/**
	 * List connection log events for a device to aid in diagnosing connectivity problems. 
	 * The entries record successful connection, unsuccessful connection attempts, 
	 * intentional disconnection and server-initiated disconnection.
	 * 
	 * <p> Refer to the
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Problem_Determination/get_logs_connection">link</a>
	 * for more information about the JSON response.</p>
	 * 
	 * @param deviceType String which contains device type
	 * @param deviceId String which contains device id
	 * 
	 * @return JsonArray
	 * @throws IOException 
	 */
	public JsonArray getDeviceConnectionLogs(String deviceType, String deviceId) throws IoTFCReSTException {
		final String METHOD = "getDeviceConnectionLogs";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/logs/connection");
		
		// add the query parameters
		
		sb.append("?typeId=").
		   append(deviceType).
		   append("&deviceId=").
		   append(deviceId);
		
		int code = 0;
		HttpResponse response = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				JsonElement jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonArray();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the connection logs "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 401) {
			throw new IoTFCReSTException(code, "The authentication token is empty or invalid");
		} else if(code == 403) {
			throw new IoTFCReSTException(code, "The authentication method is invalid or the API key used does not exist");
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "The device type does not exist");
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Register multiple new devices, each request can contain a maximum of 512KB. 
	 * The response body will contain the generated authentication tokens for all devices. 
	 * The caller of the method must make sure to record these tokens when processing 
	 * the response. The IBM Watson IoT Platform will not be able to retrieve lost authentication tokens
	 * 
	 * @param arryOfDevicesToBeAdded Array of JSON devices to be added. Refer to 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Bulk_Operations/post_bulk_devices_add">link</a> 
	 * for more information about the schema to be used
	 * 
	 * @return JsonArray containing the generated authentication tokens for all the devices 
	 * for all devices. 
	 *  
	 * @throws IoTFCReSTException 
	 */

	public JsonArray addMultipleDevices(JsonArray arryOfDevicesToBeAdded) throws IoTFCReSTException {
		final String METHOD = "bulkDevicesAdd";		
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/bulk/devices/add");

		int code = 0;
		JsonElement jsonResponse = null;
		HttpResponse response = null;
		try {
			response = connect("post", sb.toString(), arryOfDevicesToBeAdded.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code != 500) {
				// success
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
			}
			if(code == 201) {
				return jsonResponse.getAsJsonArray();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in adding the Devices, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 202) {
			throw new IoTFCReSTException(202, "Some devices registered successfully", jsonResponse);
		} else if(code == 400) {
			throw new IoTFCReSTException(400, "Invalid request (No body, invalid JSON, unexpected key, bad value)", jsonResponse);
		} else if(code == 403) {
			throw new IoTFCReSTException(403, "Maximum number of devices exceeded", jsonResponse);
		} else if(code == 413) {
			throw new IoTFCReSTException(413, "Request content exceeds 512Kb", jsonResponse);
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		
		throw new IoTFCReSTException(code, "",jsonResponse);
	}
	
	/**
	 * Delete multiple devices, each request can contain a maximum of 512Kb
	 * 
	 * @param arryOfDevicesToBeDeleted Array of JSON devices to be deleted. Refer to 
	 * <a href="https://docs.internetofthings.ibmcloud.com/swagger/v0002.html#!/Bulk_Operations/post_bulk_devices_remove">link</a> 
	 * for more information about the schema to be used.
	 * 
	 * @return JsonArray containing the status of the operations for all the devices
	 *  
	 * @throws IoTFCReSTException
	 */
	public JsonArray deleteMultipleDevices(JsonArray arryOfDevicesToBeDeleted) throws IoTFCReSTException {
		final String METHOD = "bulkDevicesRemove";
		/**
		 * Form the url based on this swagger documentation
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/bulk/devices/remove");

		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("post", sb.toString(), arryOfDevicesToBeDeleted.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code != 500) {
				// success
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
			}
			if(code == 201) {
				return jsonResponse.getAsJsonArray();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Devices, "
					+ ":: "+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 202) {
			throw new IoTFCReSTException(202, "Some devices deleted successfully", jsonResponse);
		} else if(code == 400) {
			throw new IoTFCReSTException(400, "Invalid request (No body, invalid JSON, unexpected key, bad value)", jsonResponse);
		} else if(code == 413) {
			throw new IoTFCReSTException(413, "Request content exceeds 512Kb", jsonResponse);
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error");
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	
	/**
	 * Gets a list of device management requests, which can be in progress or recently completed.
	 * 
	 * @return JSON response containing the list of device management requests.
	 *  
	 * @throws IoTFCReSTException 
	 */
	
	public JsonObject getAllDeviceManagementRequests() throws IoTFCReSTException {
		return getAllDeviceManagementRequests((ArrayList<NameValuePair>)null);
	}
	
	
	/**
	 * Gets a list of device management requests, which can be in progress or recently completed.
	 * 
	 * @param parameters list of query parameters that controls the output.
	 * 
	 * @return JSON response containing the list of device management requests.
	 * @throws IoTFCReSTException
	 */
	public JsonObject getAllDeviceManagementRequests(ArrayList<NameValuePair> parameters) throws IoTFCReSTException {
		final String METHOD = "getAllDeviceManagementRequests";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/mgmt/requests");
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			if(code == 200) {
				String result = this.readContent(response, METHOD);
				jsonResponse = new JsonParser().parse(result);
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in getting the Device management Requests "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error", jsonResponse);
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Initiates a device management request, such as reboot.
	 * 
	 * @param JsonObject JSON object containing the management request
	 * 
	 * @return JSON response containing the newly initiated request.
	 *  
	 * @throws IoTFCReSTException 
	 */
	public boolean initiateDeviceManagementRequest(JsonObject request) throws IoTFCReSTException {
		final String METHOD = "initiateDeviceManagementRequest";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/mgmt/requests");
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("post", sb.toString(), request.toString(), null);
			code = response.getStatusLine().getStatusCode();
			if(code == 202) {
				return true;
			}
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in initiating the Device management Request "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error", jsonResponse);
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	

	/**
	 * Clears the status of a device management request. The status for a 
	 * request that has been completed is automatically cleared soon after 
	 * the request completes. You can use this operation to clear the status 
	 * for a completed request, or for an in-progress request which may never 
	 * complete due to a problem.
	 * 
	 * @param requestId String ID representing the management request
	 * @return JSON response containing the newly initiated request.
	 *  
	 * @throws IoTFCReSTException 
	 */
	public boolean deleteDeviceManagementRequest(String requestId) throws IoTFCReSTException {
		String METHOD = "deleteDeviceManagementRequest";
		/**
		 * Form the url based on the swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/mgmt/requests/").append(requestId);

		int code = 0;
		JsonElement jsonResponse = null;
		HttpResponse response = null;
		try {
			response = connect("delete", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			if(code == 204) {
				return true;
			}
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Device management Request "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error", jsonResponse);
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	
	/**
	 * Gets details of a device management request.
	 * 
	 * @param requestId String ID representing the management request
	 * @return JSON response containing the device management request
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDeviceManagementRequest(String requestId) throws IoTFCReSTException {
		final String METHOD = "getDeviceManagementRequest";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/mgmt/requests/").append(requestId);
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in deleting the Device management Request "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error", jsonResponse);
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "Request status not found");
		}
		throwException(response, METHOD);
		return null;
	}

	/**
	 * Get a list of device management request device statuses
	 * 
	 * @param requestId String ID representing the management request
	 * @param parameters list of query parameters that controls the output.
	 * 
	 * @return JSON response containing the device management request
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDeviceManagementRequestStatus(String requestId, 
			ArrayList<NameValuePair> parameters) throws IoTFCReSTException {
		
		final String METHOD = "getDeviceManagementRequestStatus";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/mgmt/requests/").
		   append(requestId).
		   append("/deviceStatus");
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the Device management Request "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error", jsonResponse);
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "Request status not found", jsonResponse);
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Get a list of device management request device statuses
	 * 
	 * @param requestId String ID representing the management request
	 * @return JSON response containing the device management request
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDeviceManagementRequestStatus(String requestId) throws IoTFCReSTException {
		return getDeviceManagementRequestStatus(requestId, null);
	}


	/**
	 * Get an individual device mangaement request device status
	 * 
	 * @param requestId String ID representing the management request
	 * @param parameters list of query parameters that controls the output.
	 * 
	 * @return JSON response containing the device management request
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDeviceManagementRequestStatusByDevice(String requestId, 
			String deviceType, String deviceId) throws IoTFCReSTException {
		
		final String METHOD = "getDeviceManagementRequestStatusByDevice";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/mgmt/requests/").
		   append(requestId).
		   append("/deviceStatus/").
		   append(deviceType).
		   append('/').
		   append(deviceId);
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the Device management Request "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error", jsonResponse);
		} else if(code == 404) {
			throw new IoTFCReSTException(code, "Request status not found", jsonResponse);
		}
		throwException(response, METHOD);
		return null;
	}
	
	/**
	 * Retrieve the number of active devices over a period of time
	 * 
	 * @param startDate Start date in one of the following formats: YYYY (last day of the year), 
	 * YYYY-MM (last day of the month), YYYY-MM-DD (specific day)
	 * 
	 * @param endDate End date in one of the following formats: YYYY (last day of the year), 
	 * YYYY-MM (last day of the month), YYYY-MM-DD (specific day)
	 * 
	 * @param detail Indicates whether a daily breakdown will be included in the resultset
	 * 
	 * @return JSON response containing the active devices over a period of time
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getActiveDevices(String startDate, String endDate, boolean detail) throws IoTFCReSTException {
		final String METHOD = "getActiveDevices";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/usage/active-devices");
		
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		if(startDate != null) {
			parameters.add(new BasicNameValuePair("start", startDate));
		}
		if(endDate != null) {
			parameters.add(new BasicNameValuePair("end", endDate));
		}
		parameters.add(new BasicNameValuePair("detail", Boolean.toString(detail)));
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the Active Devices "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 400) {
			throw new IoTFCReSTException(code, "Bad Request", jsonResponse);
		} else if (code == 500) {
			throw new IoTFCReSTException(code, "Unexpected error", jsonResponse);
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	
	/**
	 * Retrieve the amount of storage being used by historical event data
	 * 
	 * @param startDate Start date in one of the following formats: YYYY (last day of the year), 
	 * YYYY-MM (last day of the month), YYYY-MM-DD (specific day)
	 * 
	 * @param endDate End date in one of the following formats: YYYY (last day of the year), 
	 * YYYY-MM (last day of the month), YYYY-MM-DD (specific day)
	 * 
	 * @param detail Indicates whether a daily breakdown will be included in the resultset
	 * 
	 * @return JSON response containing the active devices over a period of time
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getHistoricalDataUsage(String startDate, String endDate, boolean detail) throws IoTFCReSTException {
		final String METHOD = "getHistoricalDataUsage";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/usage/historical-data");
		
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		if(startDate != null) {
			parameters.add(new BasicNameValuePair("start", startDate));
		}
		if(endDate != null) {
			parameters.add(new BasicNameValuePair("end", endDate));
		}
		parameters.add(new BasicNameValuePair("detail", Boolean.toString(detail)));
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the historical data storage "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 400) {
			throw new IoTFCReSTException(code, "Bad Request", jsonResponse);
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error", jsonResponse);
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	
	/**
	 * Retrieve the amount of data used
	 * 
	 * @param startDate Start date in one of the following formats: YYYY (last day of the year), 
	 * YYYY-MM (last day of the month), YYYY-MM-DD (specific day)
	 * 
	 * @param endDate End date in one of the following formats: YYYY (last day of the year), 
	 * YYYY-MM (last day of the month), YYYY-MM-DD (specific day)
	 * 
	 * @param detail Indicates whether a daily breakdown will be included in the resultset
	 * 
	 * @return JSON response containing the active devices over a period of time
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getDataTraffic(String startDate, String endDate, boolean detail) throws IoTFCReSTException {
		final String METHOD = "getDataTraffic";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/usage/data-traffic");
		
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		if(startDate != null) {
			parameters.add(new BasicNameValuePair("start", startDate));
		}
		if(endDate != null) {
			parameters.add(new BasicNameValuePair("end", endDate));
		}
		parameters.add(new BasicNameValuePair("detail", Boolean.toString(detail)));
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, parameters);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the data traffic "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if(code == 400) {
			throw new IoTFCReSTException(code, "Bad Request", jsonResponse);
		} else if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error", jsonResponse);
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}
	
	/**
	 * Retrieve the status of services for an organization
	 * 
	 * @return JSON response containing the status of services for an organization
	 *  
	 * @throws IoTFCReSTException 
	 */
	public JsonObject getServiceStatus() throws IoTFCReSTException {
		final String METHOD = "getServiceStatus";
		/**
		 * Form the url based on this swagger documentation
		 * 
		 */
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).
		   append('.').
		   append(BASIC_API_V0002_URL).
		   append("/service-status");
		
		int code = 0;
		HttpResponse response = null;
		JsonElement jsonResponse = null;
		try {
			response = connect("get", sb.toString(), null, null);
			code = response.getStatusLine().getStatusCode();
			String result = this.readContent(response, METHOD);
			jsonResponse = new JsonParser().parse(result);
			if(code == 200) {
				return jsonResponse.getAsJsonObject();
			}
		} catch(Exception e) {
			IoTFCReSTException ex = new IoTFCReSTException("Failure in retrieving the service status "
					+ "::"+e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		
		if (code == 500) {
			throw new IoTFCReSTException(500, "Unexpected error", jsonResponse);
		}
		throw new IoTFCReSTException(code, "", jsonResponse);
	}

	/**
	 * Register a new device under the given gateway.
	 *  
	 * The response body will contain the generated authentication token for the device. 
	 * The caller of the method must make sure to record the token when processing 
	 * the response. The IBM Watson IoT Platform will not be able to retrieve lost authentication tokens.
	 * 
	 * @param deviceType DeviceType ID
	 * @param deviceId device to be added.
	 * @param gatewayTypeId The device type of the gateway
	 * @param gatewayId The deviceId of the gateway
	 * 
	 * @return JsonObject containing the generated authentication token for the device. 
	 *  
	 * @throws IoTFCReSTException 
	 */

	public JsonObject registerDeviceUnderGateway(String deviceType, String deviceId,
			String gwTypeId, String gwDeviceId) throws IoTFCReSTException {
		
		JsonObject deviceObj = new JsonObject();
		deviceObj.addProperty("deviceId", deviceId);
		deviceObj.addProperty("gatewayId", gwDeviceId);
		deviceObj.addProperty("gatewayTypeId", gwTypeId);
		
		return this.registerDevice(deviceType, deviceObj);
	}
}

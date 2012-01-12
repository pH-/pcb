package com.ph.patientLogin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

// POJO, no interface no extends

// The class registers its methods for the HTTP GET request using the @GET annotation. 
// Using the @Produces annotation, it defines that it can deliver several MIME types,
// text, XML and HTML. 

// The browser requests per default the HTML MIME type.

//Sets the path to base URL + /hello
@Path("/checkin")
public class Checkin {

	static String[] line;
	// static Connection conn = null;
	static Statement stmt = null;

	// This method is called if TEXT_PLAIN is request
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String checkDatabasePlain(@QueryParam("userid") String userId,
			@DefaultValue("nopwd")   @QueryParam("pwd") String password,
			@DefaultValue("N")		 @QueryParam("staff") String staffIndicator) throws SQLException {
		if (password.equalsIgnoreCase("nopwd")) {
			PatientRecord patientData = getFromDb(userId);
			return patientData.getName()+","+patientData.getGender()+","+ patientData.getInsuranceNum() +","+ patientData.getInsuranceCompany() +","+ patientData.getInsuranceType() +","+patientData.getHealthHistory()+","+patientData.getChiefComplaint();
		} else {
			boolean valid = checkDb(userId, password,staffIndicator);
			if (valid)
				return "validPwd";
			else
				return "invalidPwd";
		}
	}
//	@GET
//	@Produces(MediaType.TEXT_PLAIN)
//	@Consumes("staffLogin")
//	public String validateStaff(@QueryParam("userid") String userId, @QueryParam("pwd") String password, @QueryParam("staff") char staffIndicator)
//	{
//		boolean valid = checkStaffDb(userId,password,staffIndicator);
//		if(valid)
//			return "validPwd";
//		else
//			return "invalidPwd";
//	}
	@POST
	@Consumes({MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.TEXT_XML, "application/json","application/x-www-form-urlencoded"})
	public void addNewPatient(String jsonPost) throws JSONException, SQLException
	{
		if(jsonPost.contains("insuranceNum"))
		{
			JSONObject json = new JSONObject(jsonPost);        		
			PatientRecord newRecord=new PatientRecord();
			newRecord.setIdentity(json.getString("id"));
			newRecord.setInsuranceNum(json.getString("insuranceNum"));
			newRecord.setInsuranceCompany(json.getString("insuranceCompany"));
			newRecord.setInsuranceType(json.getString("insuranceType"));
			putInsuranceInDb(newRecord);
		}
		else if(jsonPost.contains("name"))
		{
			JSONObject json = new JSONObject(jsonPost);        
			PatientRecord newRecord=new PatientRecord();
			newRecord.setIdentity(json.getString("id"));
			newRecord.setName(json.getString("name"));
			newRecord.setGender(json.getString("gender"));
			putNamedataInDb(newRecord);
		}
		else if(jsonPost.contains("healthHistory"))
		{
			JSONObject json = new JSONObject(jsonPost);        		
			PatientRecord newRecord=new PatientRecord();
			newRecord.setIdentity(json.getString("id"));
			newRecord.setHealthHistory(json.getString("healthHistory"));
			updatePatientHistory(newRecord);
		}
		else
		{
			JSONObject json = new JSONObject(jsonPost);        	
			PatientRecord newRecord=new PatientRecord();
			newRecord.setIdentity(json.getString("id"));
			newRecord.setChiefComplaint(json.getString("chiefComplaint"));
			putComplaintInDb(newRecord);
		}
	}
	// This method is called if XML is request
	@GET
	@Produces(MediaType.TEXT_XML)
	public String checkDatabaseXML() {
		return "<?xml version=\"1.0\"?>" + "<hello> Hello pHxml" + "</hello>";
	}

	// This method is called if HTML is request
	@GET
	// @Path("/{user}/{pwd}");
	@Produces(MediaType.TEXT_HTML)
	public String checkDatabaseHTML(@QueryParam("userid") String userId,
			@DefaultValue("nopwd")   @QueryParam("pwd") String password,
			@DefaultValue("N")		 @QueryParam("staff") String staffIndicator) throws SQLException {
		if (password.equalsIgnoreCase("nopwd")) {
			PatientRecord patientData = getFromDb(userId);
			return patientData.getName()+","+patientData.getGender()+","+ patientData.getInsuranceNum() +","+ patientData.getInsuranceCompany() +","+ patientData.getInsuranceType() +","+patientData.getHealthHistory()+","+patientData.getChiefComplaint();
		} else {
			boolean valid = checkDb(userId, password,staffIndicator);
			if (valid)
				return "validPwd";
			else
				return "invalidPwd";
		}

	}
	private void putInsuranceInDb(PatientRecord newRecord) throws SQLException
	{
		Connection conn = connectToDb();
		try {
			PreparedStatement pst = conn
					.prepareStatement("INSERT into patientRecord (iD,insuranceNumber, insuranceCompany, insuranceType) VALUES (?,?,?,?)");
			PreparedStatement checkRecord = conn
					.prepareStatement("SELECT COUNT(*) as c from patientRecord where iD=?");
			checkRecord.setString(1, newRecord.getIdentity());
			checkRecord.execute();
			ResultSet checkResult = checkRecord.getResultSet();
			int count=0;
			while(checkResult.next())
			{
				count = checkResult.getInt("c");
			}
			if(count==0)
			{
				PreparedStatement loginEntry = conn.prepareStatement("INSERT into LoginDetails (loginId, password) VALUES (?,MD5(?))");
				loginEntry.setString(1, newRecord.getIdentity());
				loginEntry.setString(2, "password");
				loginEntry.execute();
				pst.setString(1, newRecord.getIdentity());
				pst.setString(2, newRecord.getInsuranceNum());
				pst.setString(3, newRecord.getInsuranceCompany());
				pst.setString(4, newRecord.getInsuranceType());
				pst.execute();
				pst.close();
				loginEntry.close();
//				ResultSet rs = pst.getResultSet();
			}
			else
			{
				PreparedStatement updateStmt = conn
						.prepareStatement("UPDATE patientRecord SET insuranceNumber=?, insuranceCompany=?, insuranceType=?  WHERE iD=?");
				updateStmt.setString(1, newRecord.getInsuranceNum());
				updateStmt.setString(2, newRecord.getInsuranceCompany());
				updateStmt.setString(3, newRecord.getInsuranceType());
				updateStmt.setString(4, newRecord.getIdentity());
				updateStmt.execute();
				updateStmt.close();
			}
			checkResult.close();
			checkRecord.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error : MySQL behaved badly, cant access patient details");
		}
		conn.close();
	}
	private void putNamedataInDb(PatientRecord newRecord) throws SQLException
	{
		Connection conn = connectToDb();
		try {
			PreparedStatement pst = conn
					.prepareStatement("INSERT into patientRecord (iD,name, gender) VALUES (?,?,?)");
			PreparedStatement checkRecord = conn
					.prepareStatement("SELECT COUNT(*) as c from patientRecord where iD=?");
			checkRecord.setString(1, newRecord.getIdentity());
			checkRecord.execute();
			ResultSet checkResult = checkRecord.getResultSet();
			int count=0;
			while(checkResult.next())
			{
				count = checkResult.getInt("c");
			}
			if(count==0)
			{
				PreparedStatement loginEntry = conn.prepareStatement("INSERT into LoginDetails (loginId, password) VALUES (?,MD5(?))");
				loginEntry.setString(1, newRecord.getIdentity());
				loginEntry.setString(2, "password");
				loginEntry.execute();
				pst.setString(1, newRecord.getIdentity());
				pst.setString(2, newRecord.getName());
				pst.setString(3, newRecord.getGender());
				pst.execute();
				pst.close();
				loginEntry.close();
			}
			else
			{
				PreparedStatement updateStmt = conn
						.prepareStatement("UPDATE patientRecord SET name=?, gender=?  WHERE iD=?");
				updateStmt.setString(1, newRecord.getName());
				updateStmt.setString(2, newRecord.getGender());
				updateStmt.setString(3, newRecord.getIdentity());
				updateStmt.execute();
				updateStmt.close();
			}
			checkResult.close();
			checkRecord.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error : MySQL behaved badly, cant access patient details");
		}
		conn.close();
	}
	private void putComplaintInDb(PatientRecord newRecord) throws SQLException
	{
		Connection conn = connectToDb();
		try {
			
			PreparedStatement checkRecord = conn
					.prepareStatement("SELECT COUNT(*) as c from patientRecord where iD=?");
			checkRecord.setString(1, newRecord.getIdentity());
			checkRecord.execute();
			ResultSet checkResult = checkRecord.getResultSet();
			int count=0;
			while(checkResult.next())
			{
				count = checkResult.getInt("c");
			}
			if(count==0)
			{
				PreparedStatement pst = conn
						.prepareStatement("INSERT into patientRecord (iD,chiefComplaint) VALUES (?,?)");
				PreparedStatement loginEntry = conn.prepareStatement("INSERT into LoginDetails (loginId, password) VALUES (?,MD5(?))");
				loginEntry.setString(1, newRecord.getIdentity());
				loginEntry.setString(2, "password");
				loginEntry.execute();
				pst.setString(1, newRecord.getIdentity());
				pst.setString(2, newRecord.getChiefComplaint());
				pst.execute();
				pst.close();
				loginEntry.close();
			}
			else
			{
				PreparedStatement updateStmt = conn
						.prepareStatement("UPDATE patientRecord SET chiefComplaint=?  WHERE iD=?");
				updateStmt.setString(1, newRecord.getChiefComplaint());
				updateStmt.setString(2, newRecord.getIdentity());
				updateStmt.execute();
				updateStmt.close();
			}
			checkResult.close();
			checkRecord.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error : MySQL behaved badly, cant access patient details");
		}
		conn.close();
	}
	private void updatePatientHistory(PatientRecord newRecord) throws SQLException
	{
		
		Connection conn = connectToDb();
		try {
			
			PreparedStatement checkRecord = conn
					.prepareStatement("SELECT COUNT(*) as c from patientRecord where iD=?");
			checkRecord.setString(1, newRecord.getIdentity());
			checkRecord.execute();
			ResultSet checkResult = checkRecord.getResultSet();
			int count=0;
			while(checkResult.next())
			{
				count = checkResult.getInt("c");
			}
			if(count==0)
			{
				PreparedStatement pst = conn
						.prepareStatement("INSERT into patientRecord (iD,healthHistory) VALUES (?,?)");
				PreparedStatement loginEntry = conn.prepareStatement("INSERT into LoginDetails (loginId, password) VALUES (?,MD5(?))");
				loginEntry.setString(1, newRecord.getIdentity());
				loginEntry.setString(2, "password");
				loginEntry.execute();
				pst.setString(1, newRecord.getIdentity());
				pst.setString(2, newRecord.getHealthHistory());
				pst.execute();
				pst.close();
				loginEntry.close();
			}
			else
			{
				PreparedStatement oldStmt = conn.prepareStatement("SELECT * from patientRecord where iD=?");
				PreparedStatement updateStmt = conn.prepareStatement("UPDATE patientRecord SET healthHistory=?  WHERE iD=?");
				oldStmt.setString(1, newRecord.getIdentity());
				oldStmt.execute();
				ResultSet oldDataValue = oldStmt.getResultSet();
				String oldData = null, newData = null;
				while(oldDataValue.next())
				{
					oldData = oldDataValue.getString("healthHistory");
					oldData = oldData.concat(" ");
					newData = oldData.concat(newRecord.getHealthHistory());
				}
				updateStmt.setString(1, newData);
				updateStmt.setString(2, newRecord.getIdentity());
				updateStmt.execute();
				oldStmt.close();
				oldDataValue.close();
				updateStmt.close();
			}
			checkResult.close();
			checkRecord.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error : MySQL behaved badly, cant access patient details");
		}
		conn.close();
	}
	private PatientRecord getFromDb(String id) throws SQLException {
//		String dbName = "jdbc:mysql://127.0.0.1:3306/hospitalCheckin";
//		String uname = "pH";
//		String pwd = "pH";
		Connection conn = connectToDb();
		PatientRecord record = new PatientRecord();
		try {
			PreparedStatement pst = conn
					.prepareStatement("SELECT * from patientRecord where iD=?");
			pst.setString(1, id);
			pst.execute();
			ResultSet rs = pst.getResultSet();
			while (rs.next()) {
				record.setName(rs.getString("name"));
				record.setGender(rs.getString("gender"));
				record.setInsuranceNum(rs.getString("insuranceNumber"));
				record.setInsuranceCompany(rs.getString("insuranceCompany"));
				record.setInsuranceType(rs.getString("insuranceType"));
				record.setHealthHistory(rs.getString("healthHistory"));
				record.setChiefComplaint(rs.getString("chiefComplaint"));
			}
			pst.close();
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error : MySQL behaved badly, cant access patient details");
		}
		conn.close();
		return record;
	}

	private boolean checkDb(String id, String password,String staffIndicator) throws SQLException {
//		String dbName = "jdbc:mysql://localhost:3306/hospitalCheckin";
//		String uname = "pH";
//		String pwd = "pH";
		Connection conn = connectToDb();
		try {
			PreparedStatement pst = conn.prepareStatement("SELECT * from LoginDetails where loginId=? and staffId=?");
			pst.setString(1, id);
			pst.setString(2, staffIndicator);
			pst.execute();
			ResultSet rs = pst.getResultSet();
			while (rs.next()) {
				if (password.equalsIgnoreCase(rs.getString("password")))
				{
					pst.close();
					rs.close();
					conn.close();
					return true;
				}
			}
			pst.close();
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			System.err
					.println("Error : MySQL behaved badly, cant access login details");
		}
		conn.close();
		return false;
	}
//	private boolean checkStaffDb(String userId, String password)
//	{
//		Connection conn = connectToDb();
//		try {
//			preparedStatement pwd = conn.prepareStatement("SELECT * from LoginDetails where loginId=?,");
//		}
//	}
	private static Connection connectToDb() {
		Connection conn = null;
		try {
			Context initCtx =  new InitialContext();
			DataSource ds  = (DataSource) initCtx.lookup("java:comp/env/jdbc/hospitalCheckin");
			conn=ds.getConnection();
		} 
		catch (NamingException e) {
			e.printStackTrace();
			System.err.println("Error : MySQL behaved badly, cannot connect");
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error : MySQL behaved badly, cannot connect");
		}
		return conn;
	}
}
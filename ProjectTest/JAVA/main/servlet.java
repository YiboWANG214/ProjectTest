// servlet/registration.java

package projecttest.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class registration extends HttpServlet {
	private static final long serialVersionUID = 1L;
 
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		response.setContentType("text/html");  
		PrintWriter out = response.getWriter();
		
		
		String f=request.getParameter("fname");
		
		String c=request.getParameter("cardno");
		String cn=request.getParameter("cono");
		String ad=request.getParameter("add");
		String dob=request.getParameter("dob");
		String email=request.getParameter("email");
		String pin=request.getParameter("pin");
		try
		{
		
		Class.forName("com.mysql.jdbc.Driver");
		Connection con=(Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/votingdb","Vaishnavi","Nelavetla@537");
		PreparedStatement ps=con.prepareStatement("insert into register values(?,?,?,?,?,?,?)");
		ps.setString(1,f);
		
		ps.setString(2,c);
		ps.setString(3,cn);
		ps.setString(4,ad);
		ps.setString(5,dob);
		ps.setString(6,email);
		ps.setString(7,pin);
		int i=ps.executeUpdate();
		if(i>0)
		{
			out.print("Successfully your account has been created...PLEASE LOGIN");
			 RequestDispatcher rd=request.getRequestDispatcher("loginpage.html");  
		        rd.include(request,response);
		}
		else
		{
			out.print("Failed account creation try again");
			 RequestDispatcher rd=request.getRequestDispatcher("registration.html");  
		        rd.include(request,response);
		}
			
		}
		catch (Exception e2) {
			out.print("Invalid , Failed account creation try again  "+e2);
			 RequestDispatcher rd=request.getRequestDispatcher("registration.html");  
		        rd.include(request,response);
		}
		
		out.close();
	
}
	protected void service(HttpServletRequest request, HttpServletResponse   response) throws ServletException, IOException 
	{
        doPost(request, response);
	}

}


// servlet/loginpage.java

package projecttest.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class loginpage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	final static  Connection con=DBUtilR.getDBConnection();
	static PreparedStatement ps = null;
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		PrintWriter out = response.getWriter();  
		String card=request.getParameter("cardno");
		Integer pin=Integer.parseInt(request.getParameter("pin"));
		
		try {
			if(check(card,pin))
			{
				out.print("Successful Login...You Can Vote Now");
				RequestDispatcher rd=request.getRequestDispatcher("vote.html");
				 rd.include(request,response);
			}	
			else {
				 out.print("Sorry username or password error , Make new account");  
				 RequestDispatcher rd=request.getRequestDispatcher("registration.html");  
			        rd.include(request,response);
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	
	
	}
	static boolean check(String card,Integer pin) throws SQLException
	 {
		 boolean r=false;
		 ps=con.prepareStatement("Select * from register where cardno=? and pin=?");
		 ps.setString(1,card);
		 ps.setInt(2,pin);
		 ResultSet rs=ps.executeQuery();
		 r=rs.next();
		 
		 return r;
	 }
	
	static boolean checkvote(String card) throws SQLException
	 {
		 boolean r=false;
		 ps=con.prepareStatement("Select * from vote where cardno=?");
		 ps.setString(1,card);
		 
		 ResultSet rs=ps.executeQuery();
		 r=rs.next();
		 
		 return r;
	 }

}


// servlet/againvote.java

package projecttest.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class againvote extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		doGet(request, response);
	}

}


// servlet/thankyou.java

package projecttest.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class thankyou extends HttpServlet {
	private static final long serialVersionUID = 1L;
 
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		
	}

}


// servlet/DBUtilR.java

package projecttest.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtilR {
	  static Connection conn = null;
	static
	 {	
		try {
			Class.forName("com.mysql.jdbc.Driver");
			
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/votingdb", "Vaishnavi", "Nelavetla@537");
			
			if(!conn.isClosed()) {
				System.out.println("Connection established");
			}
			
		} catch (ClassNotFoundException | SQLException e) {
			System.out.println("Error in DBUtilFile");
			e.printStackTrace();
		}
	}
	
	public static  Connection getDBConnection() {
		// TODO Auto-generated method stub
		return conn;
	}
}

// servlet/vote.java

package projecttest.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class vote extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    final static Connection con=DBUtilR.getDBConnection();
    static PreparedStatement ps = null;
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		response.setContentType("text/html");  
		PrintWriter out = response.getWriter();
		
		
		String f=request.getParameter("cardno");
		String l=request.getParameter("party");
		try
		{
		
		Class.forName("com.mysql.jdbc.Driver");
		Connection con=(Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/votingdb","Vaishnavi","Nelavetla@537");

		if(checkLogin(f))
		{
			
		ps=con.prepareStatement("insert into vote values(?,?)");
		ps.setString(1,f);
		ps.setString(2,l);
		int i=ps.executeUpdate();
		if(i>0)
		{
			out.print("Your Vote has been submitted successfully...");
			 RequestDispatcher rd=request.getRequestDispatcher("thankyou.html");  
		        rd.include(request,response);
		}
		else
		{
			out.print("Failed to submit vote, try again");
			 RequestDispatcher rd=request.getRequestDispatcher("vote.html");  
		        rd.include(request,response);
		}
		}
		else
		{
			out.print("Please enter correct card number");
			RequestDispatcher rd=request.getRequestDispatcher("vote.html");  
	        rd.include(request,response);
		}
		}
		catch (SQLIntegrityConstraintViolationException e2) {
			out.print("Please select any party");
			 RequestDispatcher rd=request.getRequestDispatcher("vote.html");  
		        rd.include(request,response);
		}
		catch(Exception e)
		{
			out.print(" " +e);
			RequestDispatcher rd=request.getRequestDispatcher("vote.html");  
	        rd.include(request,response);
		}
		out.close();
	
		
}
	protected void service(HttpServletRequest request, HttpServletResponse   response) throws ServletException, IOException {
        doPost(request, response);
}
	


static boolean checkLogin(String card) throws SQLException
{
 boolean r=false;
 ps=con.prepareStatement("Select * from register where cardno = ?");
 ps.setString(1,card);
 
 ResultSet rs=ps.executeQuery();
 r=rs.next();
 
 return r;
}
}



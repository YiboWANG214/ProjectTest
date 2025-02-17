// bankingApplication/bankManagement.java

package projecteval.bankingApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

public class bankManagement { // these class provides all
							// bank method

	private static final int NULL = 0;

	static Connection con = connection.getConnection();
	static String sql = "";
	public static boolean
	createAccount(String name,
				int passCode) // create account function
	{
		try {
			// validation
			if (name == "" || passCode == NULL) {
				System.out.println("All Field Required!");
				return false;
			}
			// query
			Statement st = con.createStatement();
			sql = "INSERT INTO customer(cname,balance,pass_code) values('"
				+ name + "',1000," + passCode + ")";

			// Execution
			if (st.executeUpdate(sql) == 1) {
				System.out.println(name
								+ ", Now You Login!");
				return true;
			}
			// return
		}
		catch (SQLIntegrityConstraintViolationException e) {
			System.out.println("Username Not Available!");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public static boolean
	loginAccount(String name, int passCode) // login method
	{
		try {
			// validation
			if (name == "" || passCode == NULL) {
				System.out.println("All Field Required!");
				return false;
			}
			// query
			sql = "select * from customer where cname='"
				+ name + "' and pass_code=" + passCode;
			PreparedStatement st
				= con.prepareStatement(sql);
			ResultSet rs = st.executeQuery();
			// Execution
			BufferedReader sc = new BufferedReader(
				new InputStreamReader(System.in));

			if (rs.next()) {
				// after login menu driven interface method

				int ch = 5;
				int amt = 0;
				int senderAc = rs.getInt("ac_no");
				;
				int receiveAc;
				while (true) {
					try {
						System.out.println(
							"Hallo, "
							+ rs.getString("cname"));
						System.out.println(
							"1)Transfer Money");
						System.out.println("2)View Balance");
						System.out.println("5)LogOut");

						System.out.print("Enter Choice:");
						ch = Integer.parseInt(
							sc.readLine());
						if (ch == 1) {
							System.out.print(
								"Enter Receiver A/c No:");
							receiveAc = Integer.parseInt(
								sc.readLine());
							System.out.print(
								"Enter Amount:");
							amt = Integer.parseInt(
								sc.readLine());

							if (bankManagement
									.transferMoney(
										senderAc, receiveAc,
										amt)) {
								System.out.println(
									"MSG : Money Sent Successfully!\n");
							}
							else {
								System.out.println(
									"ERR : Failed!\n");
							}
						}
						else if (ch == 2) {

							bankManagement.getBalance(
								senderAc);
						}
						else if (ch == 5) {
							break;
						}
						else {
							System.out.println(
								"Err : Enter Valid input!\n");
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			else {
				return false;
			}
			// return
			return true;
		}
		catch (SQLIntegrityConstraintViolationException e) {
			System.out.println("Username Not Available!");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public static void
	getBalance(int acNo) // fetch balance method
	{
		try {

			// query
			sql = "select * from customer where ac_no="
				+ acNo;
			PreparedStatement st
				= con.prepareStatement(sql);

			ResultSet rs = st.executeQuery(sql);
			System.out.println(
				"-----------------------------------------------------------");
			System.out.printf("%12s %10s %10s\n",
							"Account No", "Name",
							"Balance");

			// Execution

			while (rs.next()) {
				System.out.printf("%12d %10s %10d.00\n",
								rs.getInt("ac_no"),
								rs.getString("cname"),
								rs.getInt("balance"));
			}
			System.out.println(
				"-----------------------------------------------------------\n");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static boolean transferMoney(int sender_ac,
										int reveiver_ac,
										int amount)
		throws SQLException // transfer money method
	{
		// validation
		if (reveiver_ac == NULL || amount == NULL) {
			System.out.println("All Field Required!");
			return false;
		}
		try {
			con.setAutoCommit(false);
			sql = "select * from customer where ac_no="
				+ sender_ac;
			PreparedStatement ps
				= con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				if (rs.getInt("balance") < amount) {
					System.out.println(
						"Insufficient Balance!");
					return false;
				}
			}

			Statement st = con.createStatement();

			// debit
			con.setSavepoint();

			sql = "update customer set balance=balance-"
				+ amount + " where ac_no=" + sender_ac;
			if (st.executeUpdate(sql) == 1) {
				System.out.println("Amount Debited!");
			}

			// credit
			sql = "update customer set balance=balance+"
				+ amount + " where ac_no=" + reveiver_ac;
			st.executeUpdate(sql);

			con.commit();
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			con.rollback();
		}
		// return
		return false;
	}
}


// bankingApplication/connection.java

package projecteval.bankingApplication;

import java.sql.Connection;
import java.sql.DriverManager;
// Global connection Class
public class connection {
	static Connection con; // Global Connection Object
	public static Connection getConnection()
	{
		try {
			
			
			String mysqlJDBCDriver
				= "com.mysql.cj.jdbc.Driver"; //jdbc driver
			String url
				= "jdbc:mysql://localhost:3306/mydata"; //mysql url
			String user = "root";	 //mysql username
			String pass = "Pritesh4@"; //mysql passcode
			Class.forName(mysqlJDBCDriver);
			con = DriverManager.getConnection(url, user,
											pass);
		}
		catch (Exception e) {
			System.out.println("Connection Failed!");
		}

		return con;
	}
}


// bankingApplication/bank.java

package projecteval.bankingApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class bank {
	public static void main(String args[]) //main class of bank
		throws IOException
	{

		BufferedReader sc = new BufferedReader(
			new InputStreamReader(System.in));
		String name = "";
		int pass_code;
		int ac_no;
		int ch;

		while (true) {
			System.out.println(
				"\n ->|| Welcome to InBank ||<- \n");
			System.out.println("1)Create Account");
			System.out.println("2)Login Account");

			try {
				System.out.print("\n Enter Input:"); //user input
				ch = Integer.parseInt(sc.readLine());

				switch (ch) {
				case 1:
					try {
						System.out.print(
							"Enter Unique UserName:");
						name = sc.readLine();
						System.out.print(
							"Enter New Password:");
						pass_code = Integer.parseInt(
							sc.readLine());

						if (bankManagement.createAccount(
								name, pass_code)) {
							System.out.println(
								"MSG : Account Created Successfully!\n");
						}
						else {
							System.out.println(
								"ERR : Account Creation Failed!\n");
						}
					}
					catch (Exception e) {
						System.out.println(
							" ERR : Enter Valid Data::Insertion Failed!\n");
					}
					break;

				case 2:
					try {
						System.out.print(
							"Enter UserName:");
						name = sc.readLine();
						System.out.print(
							"Enter Password:");
						pass_code = Integer.parseInt(
							sc.readLine());

						if (bankManagement.loginAccount(
								name, pass_code)) {
							System.out.println(
								"MSG : Logout Successfully!\n");
						}
						else {
							System.out.println(
								"ERR : login Failed!\n");
						}
					}
					catch (Exception e) {
						System.out.println(
							" ERR : Enter Valid Data::Login Failed!\n");
					}

					break;

				default:
					System.out.println("Invalid Entry!\n");
				}

				if (ch == 5) {
					System.out.println(
						"Exited Successfully!\n\n Thank You :)");
					break;
				}
			}
			catch (Exception e) {
				System.out.println("Enter Valid Entry!");
			}
		}
		sc.close();
	}
}



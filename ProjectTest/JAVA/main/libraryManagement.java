// libraryManagement/LibFunctions.java

package projecteval.libraryManagement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class LibFunctions {

    public static void callIssueMenu() {
        System.out.println("Reached inside issue book menu");
        Member m = new Member();
        Book b = new Book();
        Scanner sc = new Scanner(System.in);
        int addStatus = 0;

        while (addStatus == 0) {
            try {
                System.out.println("Enter the member id ");
                m.setMemberId(Integer.parseInt(sc.nextLine().toString()));
                System.out.println("Enter the isbn code ");
                b.setIsbnCode(sc.nextLine().toString());
                issueBook(m, b);
                addStatus = 1;

            } catch (Exception e) {
                addStatus = 0;
            }

        }

    }
    

    public static void issueBook(Member m, Book b) {
        Connection conn = LibUtil.getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = null;
            String qry = "select m.member_id, b.isbn_code, mbr.rec_id from members m,books b,member_book_record mbr\n"
                    + "where m.member_id= " + m.getMemberId() + " \n"
                    + "and b.isbn_code = '" + b.getIsbnCode() + "' \n"
                    + "and m.member_id=mbr.member_id\n"
                    + "and b.isbn_code=mbr.isbn_code and mbr.dor is null ";
            rs=stmt.executeQuery(qry);
            if (rs.next()) {
                System.out.println("The book is already issued and cannot be issued again");
            } else {
                int k = stmt.executeUpdate("insert into member_book_record values(lib_seq.nextval," + m.getMemberId() + ",'" + b.getIsbnCode() + "',sysdate,null)");
                if(k > 0){
                    k = stmt.executeUpdate("update books set units_available= (units_available-1) where isbn_code = '"+ b.getIsbnCode() +"' "); 
                    conn.commit();
                    System.out.println("The book is issued successfully");
                }else{
                    conn.rollback();
                }

            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void callReturnMenu() {
        System.out.println("Reached inside return book menu");
        Member m = new Member();
        Book b = new Book();
        Scanner sc = new Scanner(System.in);
        int addStatus = 0;

        while (addStatus == 0) {
            try {
                System.out.println("Enter the member id ");
                m.setMemberId(Integer.parseInt(sc.nextLine().toString()));
                System.out.println("Enter the isbn code ");
                b.setIsbnCode(sc.nextLine().toString());
                returnBook(m, b);
                addStatus = 1;

            } catch (Exception e) {
                addStatus = 0;
            }

        }

    }
    
    public static void returnBook(Member m, Book b) {
        Connection conn = LibUtil.getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = null;
            String qry = "select m.member_id, b.isbn_code, mbr.rec_id from members m,books b,member_book_record mbr\n"
                    + "where m.member_id= " + m.getMemberId() + " \n"
                    + "and b.isbn_code = '" + b.getIsbnCode() + "' \n"
                    + "and m.member_id=mbr.member_id\n"
                    + "and b.isbn_code=mbr.isbn_code and mbr.dor is null ";
            rs=stmt.executeQuery(qry);
            if (rs.next()) {
                Integer recId= rs.getInt(3);
                System.out.println("The book is already issued and starting the process to return ");
                int k = stmt.executeUpdate("update books set units_available= (units_available+1) where isbn_code = '"+ b.getIsbnCode() +"' "); 
                if(k > 0){
                    k = stmt.executeUpdate("update member_book_record set dor= sysdate where rec_id = "+ recId +" "); 
                    conn.commit();
                    System.out.println("The book is returned successfully");
                }else{
                    conn.rollback();
                }

            } else{
                System.out.println("This book is not issued for the user");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
        
        
}


// libraryManagement/Member.java

package projecteval.libraryManagement;

import java.sql.Date;


/**
 *
 * @author testuser
 */
public class Member {

    private Integer memberId;
    private String memberName;
    private Date dateOfJoining;

    public Member() {

    }

    public Member(Integer memberId, String memberName, Date dateOfJoining) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.dateOfJoining = dateOfJoining;
    }

    public Date getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(Date dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

}


// libraryManagement/Book.java

package projecteval.libraryManagement;

/**
 *
 * @author testuser
 */
public class Book {
    private String isbnCode;
    private String bookName;
    private String bookDesc;
    private String authorName;
    private String subjectName;
    private Integer unitsAvailable;
    
    public Book(){
        
    }
    public Book(String isbnCode, String bookName, String bookDesc, String authorName, String subjectName, Integer unitsAvailable) {
        this.isbnCode = isbnCode;
        this.bookName = bookName;
        this.bookDesc = bookDesc;
        this.authorName = authorName;
        this.subjectName = subjectName;
        this.unitsAvailable = unitsAvailable;
    }

    public Integer getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(Integer unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    public String getIsbnCode() {
        return isbnCode;
    }

    public void setIsbnCode(String isbnCode) {
        this.isbnCode = isbnCode;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getBookDesc() {
        return bookDesc;
    }

    public void setBookDesc(String bookDesc) {
        this.bookDesc = bookDesc;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }


}


// libraryManagement/UserMenu.java

package projecteval.libraryManagement;

import java.util.Scanner;


/**
 *
 * @author testuser
 */
public class UserMenu {
    public static void main(String[] args) {
        String input="";
        Scanner sc = new Scanner(System.in);
        
        while(input != "5"){
            System.out.println("---------------------------------------------------------");
            System.out.println("---------------------------------------------------------");
            System.out.println("---------------------------------------------------------");

            System.out.println("Select the following options");
            System.out.println("Enter 1 for adding a book");
            System.out.println("Enter 2 for adding a member");
            System.out.println("Enter 3 for issuing a book ");
            System.out.println("Enter 4 for returning  a book ");
            System.out.println("Enter 5 to exit");
            input = processUserInput(sc.nextLine().toString());
            
        }
    }
    public static String processUserInput(String in) {
        String retVal="5";
        switch(in){
            case "1":
                System.out.println("---------------------------------------------------------");
                System.out.println("You have selected option 1 to add a book");
                AddBookMenu.addBookMenu();
                return "1";
            case "2":
                System.out.println("---------------------------------------------------------");
                System.out.println("You have selected option 2 to add a member");
                AddMemberMenu.addMemberMenu();
                return "2";
            case "3":
                System.out.println("---------------------------------------------------------");
                System.out.println("You have selected option 3 to issue a book");
                LibFunctions.callIssueMenu();
                return "3";
            case "4":
                System.out.println("---------------------------------------------------------");
                System.out.println("You have selected option 4 to return a book");
                LibFunctions.callReturnMenu();
                return "4";
            default:
                System.out.println("---------------------------------------------------------");
                System.out.println("Thanks for working on this!!");
                return "5";
        }
        
    }
}


// libraryManagement/LibUtil.java

package projecteval.libraryManagement;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author testuser
 */
public class LibUtil {
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Properties prop = new Properties();
            FileInputStream in = new FileInputStream("src/DBProperties");
            prop.load(in);
            
            String driverName= prop.getProperty("DBDriver");
            Class.forName(driverName);
            
            String dbName,user,password;
            dbName= prop.getProperty("DBName");
            user = prop.getProperty("User");
            password= prop.getProperty("Password");
            
            conn= DriverManager.getConnection(dbName, user, password);
            return conn;
        } catch (Exception e) {
        }
        return conn;
        
    }
}


// libraryManagement/AddMemberMenu.java

package projecteval.libraryManagement;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author testuser
 */
public class AddMemberMenu {

    public static void addMemberMenu() {
        System.out.println("Reached the add member menu");
        Member m = new Member();
        Scanner sc = new Scanner(System.in);
        int addStatus = 0;

        while (addStatus == 0) {
            try {
                System.out.println("Enter the member id ");
                m.setMemberId(Integer.parseInt(sc.nextLine().toString()));
                System.out.println("Enter the member name");
                m.setMemberName(sc.nextLine().toString());
                
                addMember(m);
                addStatus = 1;
                
            } catch (Exception e) {
                addStatus=0;
            }

        }

    }

    public static void addMember(Member m) { 
        System.out.println("Reached inside add member for member "+m.getMemberId());
        Connection conn = LibUtil.getConnection();
        try {
            Statement stmt = conn.createStatement();
            int k = stmt.executeUpdate("insert into members values ("+m.getMemberId()+",'"+m.getMemberName()+"',sysdate)");
            if(k>0){
                System.out.println("Added Member successfully");
                conn.commit();
            }else{
                conn.rollback();
            }
            conn.close();
        } catch (Exception e) {
        }
        
        

    }

}


// libraryManagement/AddBookMenu.java

package projecteval.libraryManagement;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author testuser
 */
public class AddBookMenu {

    public static void addBookMenu() {
        System.out.println("Reached the add book menu");
        Book b = new Book();
        Scanner sc = new Scanner(System.in);
        int addStatus = 0;

        while (addStatus == 0) {
            try {
                System.out.println("Enter the isbn code");
                b.setIsbnCode(sc.nextLine().toString());
                System.out.println("Enter the book name");
                b.setBookName(sc.nextLine().toString());
                System.out.println("Enter the book desc");
                b.setBookDesc(sc.nextLine().toString());
                System.out.println("Enter the author name");
                b.setAuthorName(sc.nextLine().toString());
                System.out.println("Enter the subject ");
                b.setSubjectName(sc.nextLine().toString());
                System.out.println("Enter the units available");
                b.setUnitsAvailable(Integer.parseInt(sc.nextLine().toString()));

                addBook(b);
                addStatus = 1;
                
            } catch (Exception e) {
                addStatus=0;
            }

        }

    }

    public static void addBook(Book b) { 
        System.out.println("Reached inside addBook for book "+b.getIsbnCode());
        Connection conn = LibUtil.getConnection();
        try {
            Statement stmt = conn.createStatement();
            int k = stmt.executeUpdate("insert into books values ('"+b.getIsbnCode()+"','"+b.getBookName()+"','"+b.getBookDesc()+"',"
                    + "'"+b.getAuthorName()+"','"+b.getSubjectName()+"',"+b.getUnitsAvailable()+")");
            if(k>0){
                System.out.println("Added Book successfully");
                conn.commit();
            }else{
                conn.rollback();
            }
            conn.close();
        } catch (Exception e) {
        }
        
        

    }

}



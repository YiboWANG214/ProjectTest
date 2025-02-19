// libraryApp/Book.java

package projecttest.libraryApp;

public class Book {
	
	private int isbn;
	private String title;
	private String author;
	private String genre;
	private int quantity;
	private int checkedOut;
	private int checkedIn;
	
	//Constructor for book object
	public Book(int isbn, String title, String author, String genre, int quantity, int checkedOut) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.genre = genre;
		this.quantity = quantity;
		this.checkedOut = checkedOut;
		this.checkedIn = quantity-checkedOut;
	}

	public int getCheckedIn() {
		return checkedIn;
	}

	public void setCheckedIn(int checkedIn) {
		this.checkedIn = checkedIn;
	}

	public void setIsbn(int isbn) {
		this.isbn = isbn;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void setCheckedOut(int checkedOut) {
		this.checkedOut = checkedOut;
	}

	//Getter Methods
	public int getIsbn() {
		return isbn;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getGenre() {
		return genre;
	}

	public int getQuantity() {
		return quantity;
	}

	public int getCheckedOut() {
		return checkedOut;
	}

	
	
	
}

 


// libraryApp/LibraryApp.java

package projecttest.libraryApp;

public class LibraryApp {
	
	BookRepository repo = new BookRepository();
	
	public void findByTitle(String title) {
		repo.searchByTitle(title);
		return;
	}
	public void findByISBN(int isbn) {
		repo.searchByISBN(isbn);
		return;
	}
	public boolean findByGenre(String genre) {
		if(repo.searchByGenre(genre))
			return true;
		else 
			return false;
	}
	
	
	public int findISBN(int isbn) {
		return repo.searchISBN(isbn);
	}
	
	public boolean withdrawBook(int isbn) {
		return repo.getBook(isbn);
	}
	
	public boolean depositBook(int isbn) {
		return repo.submitBook(isbn);
	}

	public void getStatus(int isbn) {
		repo.bookStatus(isbn);
	}
}


// libraryApp/Main.java

package projecttest.libraryApp;

import java.util.Scanner;

public class  Main{
	static Scanner scan = new Scanner(System.in);
    static LibraryApp app = new LibraryApp();

	
	public static void main(String[] args) {
       
		int userChoice=0;
        System.out.println("-----Welcome to the Library!-----\n");
        do{
        	System.out.println("\n-----------------------------------");
        	System.out.println("1. Search book by Title keyword.");
            System.out.println("2. Search book by ISBN number.");
            System.out.println("3. Search book by Genre.");
    		System.out.println("4. Book Check In");
    		System.out.println("5. Book Check Out");
    		System.out.println("6. Exit from the library.");
    		System.out.println("-----------------------------------");
    		System.out.print("\nChoose any option: ");
            
            userChoice = scan.nextInt();
            scan.nextLine();
            
            switch(userChoice){
            	case 1:
            			System.out.print("Enter the Title of Book: ");
            			app.findByTitle(scan.nextLine());
            			break;
            	case 2: 
            			System.out.println("Enter ISBN number: ");
            			app.findByISBN(scan.nextInt());
            			break;
            	case 3:
            			System.out.println("Enter Genre: ");
            			app.findByGenre(scan.nextLine());
            			break;
            	case 4:
            			checkIn();
            			break;
            	case 5:
            			checkOut();
            			break;
            	case 6:
            			System.out.println("\nThanks for visiting. \nSee you again.");
            			break;
            	default:
            			System.out.println("\nInvalid Choice!");	
            }    
        }while(userChoice!=6);
        
    }
    
	
		//Checking book In
    	private static void checkIn() {
    		System.out.println("Enter Book's ISBN number : ");
    		int isbnNum = scan.nextInt();
    		getStatus(isbnNum);
    		int bookAvailable = app.findISBN(isbnNum);
    		if(bookAvailable==1) {
    			System.out.println(isbnNum);
    			app.withdrawBook(isbnNum);
    			System.out.println("Book CheckIn successful.");
    			getStatus(isbnNum);
    		}
    		else
    			System.out.printf("Book with %d ISBN number not Found in inventory.",isbnNum);
    	}
    	
    	
    	//Checking book Out
    	private static void checkOut() {
    		System.out.println("\nEnter Book's ISBN number : ");
    		int isbnNum = scan.nextInt();
    		int bookAvailable = app.findISBN(isbnNum);
    		if(bookAvailable==1) {
    			if(app.depositBook(isbnNum))
    			System.out.println("Book CheckOut successful.");
    			else
    				System.out.println("No Space for more Books.");
    		}
    		else
    			System.out.printf("Book with %d ISBN number not Found in inventory.",isbnNum);
    	}
    	
    	private static void getStatus(int isbn) {
    		app.getStatus(isbn);
    	}
}

// libraryApp/BookRepository.java

package projecttest.libraryApp;

import java.util.ArrayList;

public class BookRepository {
	
	private ArrayList<Book> books = new ArrayList<>();
	private int booksFound = 0;
	
	//Constructor to initialize books
	public BookRepository(){
		books.add(new Book(253910,"Pride and Prejudice C", "Jane Austen", "Love",10,7));
		books.add(new Book(391520,"Programming in ANSI C", "E. Balagurusamy", "Educational",15,10));
		books.add(new Book(715332,"Shrimad Bhagavad Gita", "Krishna Dvaipayana", "Motivational",20,18));
		books.add(new Book(935141,"Java: The Complete Reference", "Herbert Schildt", "Educational",12,9));
		books.add(new Book(459901,"It", "Stephan King", "Horror",7,5));
		books.add(new Book(855141,"Disneyland", "Mickey & Minnie", "Love",10,3));
	}
	
	
	//Searching books by Title Keyword
	public void searchByTitle(String title) {
		booksFound = 0;
		for(Book book : books) {
			String bookTitle = book.getTitle();
			if(bookTitle.toLowerCase().contains(title.toLowerCase())) {
				bookDetails(book);
				booksFound++;
			}
		}
		System.out.printf("\n%d Book%s Found.\n",booksFound,booksFound>1?"s":"");
		return;
	}
	

	//Searching books by ISBN Number
	public void searchByISBN(int isbn) {
		booksFound = 0;
		for(Book book : books) {
			if(book.getIsbn()==isbn) {
				bookDetails(book);
				booksFound++;
				break;
			}
				
		}
		System.out.printf("\n%d Book%s Found.\n",booksFound,booksFound>1?"s":"");
		return;
	}
	
	
	//Searching books by Genre
	public boolean searchByGenre(String genre){
		booksFound = 0;
		for(Book book : books) {
			String bookGenre = book.getGenre();
			if(bookGenre.toLowerCase().equals(genre.toLowerCase())) {
				bookDetails(book);
				booksFound++;
			}
		}
		System.out.printf("\n%d Book%s Found.\n",booksFound,booksFound>1?"s":"");
		if(booksFound>0)
			return true;
		else 
			return false;
			
	}
	
	
	// Display Book Details
	public void bookDetails(Book book) {
		System.out.println("\n+> Book details: \n");
		System.out.println("\tTitle: "+book.getTitle()+"\n\tAuthor: "+ book.getAuthor()+"\n\tGenre: "+book.getGenre()+"\n\tISBN: "+book.getIsbn()+"\n\tQuantity: "+book.getQuantity()+"\n\tChecked Out: "+String.valueOf(book.getCheckedOut())+"\n\tAvailable: "+String.valueOf(book.getQuantity()-book.getCheckedOut()));
	}
	
	
	//Searching for ISBN number for checkIn and checkOut
	public int searchISBN(int isbn) {
		for(Book book:books)
			if(book.getIsbn()==isbn)
				return 1;
		return 0;
	}
	
	
	//withdrawing book
	public boolean getBook(int isbn) {
		for(Book book: books) {
			if(book.getIsbn()==isbn) {
				if((book.getQuantity()-book.getCheckedOut())>0) {
					book.setCheckedOut(book.getCheckedOut()+1);
					return true;
				}
			}
		}
		return false;
	}
	
	
	//submitting book
	public boolean submitBook(int isbn) {
		for(Book book: books) {
			if(book.getQuantity()>book.getCheckedIn()) {
				book.setCheckedOut(book.getCheckedOut()-1);
				return true;
			}
		}
		return false;
	}
	
	
	//Showing status of book
	public void bookStatus(int isbn) {
		for(Book book: books) {
			if(book.getIsbn()==isbn) {
				bookDetails(book);
				break;
			}
		}
	}
	
}



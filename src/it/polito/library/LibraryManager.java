package it.polito.library;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;


public class LibraryManager {

	private static class Book {
		String id;
		String title;
		boolean rented = false;

		public Book(String id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	private static class Person {
		String id;
		String firstName;
		String lastName;
		boolean rents = false;

		public Person(String id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	private static class Rental {
		String bookId;
		String readerId;
		LocalDate startingDate;
		LocalDate endingDate = null;

		public Rental(String bookId, String readerId, LocalDate startingDate) {
			this.bookId = bookId;
			this.readerId = readerId;
			this.startingDate = startingDate;
		}
	}

	private final static DateTimeFormatter LIB_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	private Integer nextBookId = 1000;
	private Integer nextReaderId = 1000;
	private Map<String, Collection<Book>> books = new HashMap<>();
	private Map<String, Book> copies = new HashMap<>();
	private Map<String, Person> readers = new HashMap<>();
	private Map<String, Collection<Rental>> rentals = new HashMap<>();
	    
    // R1: Readers and Books 
    
    /**
	 * adds a book to the library archive
	 * The method can be invoked multiple times.
	 * If a book with the same title is already present,
	 * it increases the number of copies available for the book
	 * 
	 * @param title the title of the added book
	 * @return the ID of the book added 
	 */
    public String addBook(String title) {
		books.computeIfAbsent(title, (key) -> new ArrayList<>());

		String id = nextBookId.toString();
		nextBookId++;

		Book book = new Book(id, title);
		books.get(title).add(book);
		copies.put(id, book);
		rentals.put(id, new ArrayList<>());

		return id;
    }
    
    /**
	 * Returns the book titles available in the library
	 * sorted alphabetically, each one linked to the
	 * number of copies available for that title.
	 * 
	 * @return a map of the titles liked to the number of available copies
	 */
    public SortedMap<String, Integer> getTitles() {
    	return books.entrySet().stream()
			.sorted(Comparator.comparing(Map.Entry::getKey))
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				(entry) -> entry.getValue().size(),
				(v, w) -> v,
				TreeMap::new
			));
    }
    
    /**
	 * Returns the books available in the library
	 * 
	 * @return a set of the titles liked to the number of available copies
	 */
    public Set<String> getBooks() {
        return books.values().stream()
			.flatMap(Collection::stream)
			.map((book) -> book.id)
			.collect(Collectors.toSet());
    }
    
    /**
	 * Adds a new reader
	 * 
	 * @param name first name of the reader
	 * @param surname last name of the reader
	 */
    public void addReader(String name, String surname) {
		String id = nextReaderId.toString();
		nextReaderId++;

		readers.put(id, new Person(id, name, surname));
    }
    
    
    /**
	 * Returns the reader name associated to a unique reader ID
	 * 
	 * @param readerID the unique reader ID
	 * @return the reader name
	 * @throws LibException if the readerID is not present in the archive
	 */
    public String getReaderName(String readerID) throws LibException {
        Person reader = readers.get(readerID);
		if (reader == null) {
			throw new LibException("Reader not present");
		}

		return String.format("%s %s", reader.firstName, reader.lastName);
    }    
    
    
    // R2: Rentals Management
    
    
    /**
	 * Retrieves the bookID of a copy of a book if available
	 * 
	 * @param bookTitle the title of the book
	 * @return the unique book ID of a copy of the book or the message "Not available"
	 * @throws LibException  an exception if the book is not present in the archive
	 */
    public String getAvailableBook(String bookTitle) throws LibException {
		Collection<Book> book = books.get(bookTitle);
		if (book == null) {
			throw new LibException("Book not present");
		}
		
		return book.stream()
			.filter((copy) -> !copy.rented)
			.map((copy) -> copy.id)
			.findAny()
			.orElse("Not available");
    }
	
    /**
	 * Starts a rental of a specific book copy for a specific reader
	 * 
	 * @param bookID the unique book ID of the book copy
	 * @param readerID the unique reader ID of the reader
	 * @param startingDate the starting date of the rental
	 * @throws LibException  an exception if the book copy or the reader are not present in the archive,
	 * if the reader is already renting a book, or if the book copy is already rented
	 */
	public void startRental(String bookID, String readerID, String startingDate) throws LibException {
		Book book = copies.get(bookID);
		Person reader = readers.get(readerID);
		
		if (book == null || reader == null) {
			throw new LibException("Either book or reader isn't present in the archive");
		}

		if (reader.rents || book.rented) {
			throw new LibException("Either book is rented or reader already rents");
		}

		rentals.get(bookID).add(new Rental(bookID, readerID, LocalDate.parse(startingDate, LIB_FORMATTER)));
		book.rented = true;
		reader.rents = true;
    }
    
	/**
	 * Ends a rental of a specific book copy for a specific reader
	 * 
	 * @param bookID the unique book ID of the book copy
	 * @param readerID the unique reader ID of the reader
	 * @param endingDate the ending date of the rental
	 * @throws LibException  an exception if the book copy or the reader are not present in the archive,
	 * if the reader is not renting a book, or if the book copy is not rented
	 */
    public void endRental(String bookID, String readerID, String endingDate) throws LibException {
		Book book = copies.get(bookID);
		Person reader = readers.get(readerID);

		Rental rental = rentals.get(bookID).stream()
			.filter((r) -> r.endingDate == null)
			.findAny()
			.get();
		rental.endingDate = LocalDate.parse(endingDate, LIB_FORMATTER);
		book.rented = false;
		reader.rents = false;
    }
    
    
   /**
	* Retrieves the list of readers that rented a specific book.
	* It takes a unique book ID as input, and returns the readers' reader IDs and the starting and ending dates of each rental
	* 
	* @param bookID the unique book ID of the book copy
	* @return the map linking reader IDs with rentals starting and ending dates
	* @throws LibException  an exception if the book copy or the reader are not present in the archive,
	* if the reader is not renting a book, or if the book copy is not rented
	*/
    public SortedMap<String, String> getRentals(String bookID) throws LibException {
		SortedMap<String,String> rentalInfo = new TreeMap<>();
		Collection<Rental> bookRentals = rentals.get(bookID);

		if (bookRentals == null) {
			throw new LibException("No such book");
		}

		for (Rental rental : bookRentals) {
			String startingDate = rental.startingDate.format(LIB_FORMATTER);
			String endingDate = rental.endingDate != null
				? rental.endingDate.format(LIB_FORMATTER)
				: "ONGOING";
			
			rentalInfo.put(rental.readerId, String.format("%s %s", startingDate, endingDate));
		}

		return rentalInfo;
    }
    
    
    // R3: Book Donations
    
    /**
	* Collects books donated to the library.
	* 
	* @param donatedTitles It takes in input book titles in the format "First title,Second title"
	*/
    public void receiveDonation(String donatedTitles) {
    }
    
    // R4: Archive Management

    /**
	* Retrieves all the active rentals.
	* 
	* @return the map linking reader IDs with their active rentals

	*/
    public Map<String, String> getOngoingRentals() {
        return null;
    }
    
    /**
	* Removes from the archives all book copies, independently of the title, that were never rented.
	* 
	*/
    public void removeBooks() {
    }
    	
    // R5: Stats
    
    /**
	* Finds the reader with the highest number of rentals
	* and returns their unique ID.
	* 
	* @return the uniqueID of the reader with the highest number of rentals
	*/
    public String findBookWorm() {
        return null;
    }
    
    /**
	* Returns the total number of rentals by title. 
	* 
	* @return the map linking a title with the number of rentals
	*/
    public Map<String,Integer> rentalCounts() {
        return null;
    }

}

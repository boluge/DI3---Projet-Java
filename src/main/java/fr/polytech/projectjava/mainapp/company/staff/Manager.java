package fr.polytech.projectjava.mainapp.company.staff;

import fr.polytech.projectjava.mainapp.company.Company;
import fr.polytech.projectjava.mainapp.company.staff.checking.WorkDay;
import fr.polytech.projectjava.utils.Configuration;
import fr.polytech.projectjava.utils.Log;
import fr.polytech.projectjava.utils.MailUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javax.mail.MessagingException;
import java.io.*;
import java.time.LocalTime;
import java.util.Queue;

/**
 * Represent a manager in the company.
 * A manager can only manage less or equal than one department.
 * <p>
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 23/03/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-03-23
 */
public class Manager extends Employee implements Serializable
{
	private static final long serialVersionUID = -2861031212711385809L;
	private SimpleBooleanProperty managing;
	
	/**
	 * Promote an employee to a manager.
	 *
	 * @param employee The employee to promote.
	 */
	public Manager(Employee employee)
	{
		this(employee.getCompany(), employee.getLastName(), employee.getFirstName());
		if(employee.getWorkingDepartment() != null)
		{
			employee.getWorkingDepartment().addEmployee(this);
			employee.getWorkingDepartment().removeEmployee(employee);
		}
		getWorkingDays().clear();
		employee.getWorkingDays().forEach(workDay -> addWorkingDay(new WorkDay(this, workDay.getDay(), workDay.getStartTime(), workDay.getEndTime())));
		updateOvertime(null);
		updatePresence();
		employee.getCompany().removeEmployee(employee);
		getCompany().addEmployee(this);
	}
	
	/**
	 * Construct a manager with his/her name and his affected department.
	 *
	 * @param company   The company the employee is from.
	 * @param lastName  His/her last name.
	 * @param firstName His/her first name.
	 *
	 * @throws IllegalArgumentException If the arrival time is after the departure time.
	 */
	public Manager(Company company, String lastName, String firstName) throws IllegalArgumentException
	{
		super(company, lastName, firstName);
		managing = new SimpleBooleanProperty(false);
	}
	
	/**
	 * Create an manager with his/her name and its departure and arrival times.
	 *
	 * @param company       The company the employee is from.
	 * @param lastName      His/her last name.
	 * @param firstName     His/her first name.
	 * @param arrivalTime   The arrival time.
	 * @param departureTIme The departure time.
	 *
	 * @throws IllegalArgumentException If the arrival time is after the departure time.
	 */
	public Manager(Company company, String lastName, String firstName, LocalTime arrivalTime, LocalTime departureTIme) throws IllegalArgumentException
	{
		super(company, lastName, firstName, arrivalTime, departureTIme);
		managing = new SimpleBooleanProperty(false);
	}
	
	/**
	 * Serialize the object.
	 *
	 * @param oos The object stream.
	 *
	 * @throws IOException If the serialization failed.
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException
	{
		oos.writeBoolean(isManaging());
	}
	
	/**
	 * Return the managing status of the manager.
	 *
	 * @return True he/she is managing a department, false otherwise.
	 */
	public boolean isManaging()
	{
		return managingProperty().get();
	}
	
	/**
	 * Get the managing property.
	 *
	 * @return The managing property.
	 */
	private SimpleBooleanProperty managingProperty()
	{
		return managing;
	}
	
	/**
	 * Set the managing status of the manager.
	 *
	 * @param managing The status to set.
	 */
	public void setManaging(boolean managing)
	{
		this.managing.set(managing);
		Log.info("Manager " + this + " is " + (managing ? "now managing" : " no longer managing"));
	}
	
	/**
	 * Deserialize an object.
	 *
	 * @param ois The object stream.
	 *
	 * @throws IOException            If the deserialization failed.
	 * @throws ClassNotFoundException If the file doesn't represent the correct class.
	 */
	@SuppressWarnings("RedundantThrows")
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException
	{
		managing = new SimpleBooleanProperty(ois.readBoolean());
	}
	
	/**
	 * Constructor used to parse a manager from CSV.
	 *
	 * @param company The company the manager is from.
	 */
	protected Manager(Company company)
	{
		super(company);
		this.managing = new SimpleBooleanProperty(false);
	}
	
	/**
	 * Read a manager from the CSV.
	 *
	 * @param company The company the manager is from.
	 * @param csv     The CSV parts to read.
	 *
	 * @return The created manager.
	 */
	@SuppressWarnings("UnusedReturnValue")
	public static Manager fromCSV(Company company, Queue<String> csv)
	{
		Manager manager = new Manager(company);
		company.addEmployee(manager);
		manager.parseCSV(csv);
		if(manager.getWorkingDepartment() != null)
		{
			manager.getWorkingDepartment().addEmployee(manager);
			if(manager.isManaging())
				manager.getWorkingDepartment().setLeader(manager);
		}
		return manager;
	}
	
	@Override
	protected void parseCSV(Queue<String> csv)
	{
		super.parseCSV(csv);
		setManaging(Boolean.parseBoolean(csv.poll()));
	}
	
	@Override
	public String asCSV(String delimiter)
	{
		String csv = super.asCSV(delimiter);
		return csv + delimiter + Boolean.toString(isManaging());
	}
	
	/**
	 * Send a mail to the manager.
	 *
	 * @param object The object of the mail.
	 * @param body   The content of the mail.
	 */
	public void mailManager(String object, String body)
	{
		if(!getMail().equals("") && isValidMail())
			try
			{
				MailUtils.sendMail(Configuration.getString("smtpFrom"), "ManagementApplication", getMail(), object, body);
			}
			catch(UnsupportedEncodingException | MessagingException e)
			{
				Log.error("Failed to send mail to manager " + this, e);
			}
	}
}

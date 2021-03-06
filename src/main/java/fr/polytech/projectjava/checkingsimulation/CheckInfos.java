package fr.polytech.projectjava.checkingsimulation;

import fr.polytech.projectjava.utils.jfx.SimpleLocalDateTimeProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represent the checking information.
 * <p>
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 25/04/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-04-25
 */
public class CheckInfos implements Serializable
{
	private transient static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final long serialVersionUID = -3180459411345585955L;
	private Employee employee;
	private SimpleObjectProperty<CheckType> checkType;
	private SimpleLocalDateTimeProperty date;
	
	/**
	 * Enumeration of the different types of checks possible.
	 */
	public enum CheckType
	{
		IN, OUT
	}
	
	/**
	 * Constructor.
	 *
	 * @param employee  The employee concerned.
	 * @param checkType The type of the check.
	 * @param date      The date of the check.
	 * @param time      The time of the check.
	 */
	public CheckInfos(Employee employee, CheckType checkType, LocalDate date, LocalTime time)
	{
		this.employee = employee;
		this.checkType = new SimpleObjectProperty<>(checkType);
		this.date = new SimpleLocalDateTimeProperty(LocalDateTime.of(date, time), dateFormat);
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
		oos.writeObject(getEmployee());
		oos.writeObject(getCheckType());
		oos.writeObject(getCheckDate());
	}
	
	/**
	 * Get the employee.
	 *
	 * @return The employee.
	 */
	public Employee getEmployee()
	{
		return employee;
	}
	
	/**
	 * Deserialize an object.
	 *
	 * @param ois The object stream.
	 *
	 * @throws IOException            If the deserialization failed.
	 * @throws ClassNotFoundException If the file doesn't represent the correct class.
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException
	{
		employee = (Employee) ois.readObject();
		checkType = new SimpleObjectProperty<>((CheckType) ois.readObject());
		date = new SimpleLocalDateTimeProperty((LocalDateTime) ois.readObject(), dateFormat);
	}
	
	/**
	 * Get the checking date.
	 *
	 * @return The date.
	 */
	public LocalDateTime getCheckDate()
	{
		return dateProperty().getDate();
	}
	
	/**
	 * Get the checking type.
	 *
	 * @return The type.
	 */
	public CheckType getCheckType()
	{
		return checkTypeProperty().get();
	}
	
	/**
	 * Get the date property.
	 *
	 * @return The date property.
	 */
	public SimpleLocalDateTimeProperty dateProperty()
	{
		return date;
	}
	
	/**
	 * Get the checking property.
	 *
	 * @return The checking property.
	 */
	public SimpleObjectProperty<CheckType> checkTypeProperty()
	{
		return checkType;
	}
	
	/**
	 * Get the string to be sent to the server.
	 *
	 * @return The string to send.
	 */
	public String getForSocket()
	{
		return getEmployee().getID() + ";" + getCheckType().toString() + ";" + getFormattedCheckDate();
	}
	
	/**
	 * Get the date as a formatted string.
	 *
	 * @return The date string.
	 */
	public String getFormattedCheckDate()
	{
		return getCheckDate().format(dateFormat);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof CheckInfos))
			return false;
		CheckInfos infos = (CheckInfos) obj;
		return infos.getCheckType().equals(getCheckType()) && infos.getEmployee().equals(getEmployee()) && infos.getCheckDate().equals(getCheckDate());
	}
	
	@Override
	public String toString()
	{
		return "Check " + getCheckType() + " of " + getEmployee() + " at " + dateFormat.format(getCheckDate());
	}
}

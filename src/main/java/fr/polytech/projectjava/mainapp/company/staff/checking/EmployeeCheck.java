package fr.polytech.projectjava.mainapp.company.staff.checking;

import fr.polytech.projectjava.mainapp.company.staff.Employee;
import fr.polytech.projectjava.utils.jfx.MinutesDuration;
import fr.polytech.projectjava.utils.jfx.RoundedLocalTimeProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represent a day check.
 * <p>
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 17/05/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-05-17
 */
public class EmployeeCheck implements Serializable
{
	private static final long serialVersionUID = 2289845323375640933L;
	private SimpleObjectProperty<Employee> employee;
	private SimpleObjectProperty<LocalDate> date;
	private RoundedLocalTimeProperty checkIn;
	private RoundedLocalTimeProperty checkOut;
	
	/**
	 * Constructor.
	 *
	 * @param employee The employee of the check.
	 * @param date     The date of the check.
	 */
	public EmployeeCheck(Employee employee, LocalDate date)
	{
		this.date = new SimpleObjectProperty<>(date);
		this.employee = new SimpleObjectProperty<>(employee);
		checkIn = new RoundedLocalTimeProperty(employee);
		checkOut = new RoundedLocalTimeProperty(employee);
	}
	
	/**
	 * Constructor.
	 *
	 * @param employee   The employee of the check.
	 * @param checkInOut A checkinout to initialize the check.
	 */
	public EmployeeCheck(Employee employee, CheckInOut checkInOut)
	{
		this.employee = new SimpleObjectProperty<>(employee);
		this.date = new SimpleObjectProperty<>(checkInOut.getDay());
		checkIn = new RoundedLocalTimeProperty(employee);
		checkOut = new RoundedLocalTimeProperty(employee);
		if(checkInOut.getCheckType() == CheckInOut.CheckType.IN)
			setIn(checkInOut.getTime());
		else
			setOut(checkInOut.getTime());
	}
	
	/**
	 * Set the in check.
	 *
	 * @param check The time to set.
	 */
	public void setIn(LocalTime check)
	{
		checkIn.set(check);
	}
	
	/**
	 * Set the out check.
	 *
	 * @param check The time to set.
	 */
	public void setOut(LocalTime check)
	{
		checkOut.set(check);
	}
	
	/**
	 * Tell if the check in out is part of this day check.
	 *
	 * @param checkInOut The checkinout to test.
	 *
	 * @return True if same day, false else.
	 */
	public boolean isDateOf(CheckInOut checkInOut)
	{
		return getDate().isEqual(checkInOut.getDay());
	}
	
	/**
	 * Get the date of the check.
	 *
	 * @return The date.
	 */
	public LocalDate getDate()
	{
		return dateProperty().get();
	}
	
	/**
	 * Get the dat property.
	 *
	 * @return The day property.
	 */
	public SimpleObjectProperty<LocalDate> dateProperty()
	{
		return date;
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
		oos.writeObject(getDate());
		oos.writeInt(((checkIn.get() != null ? 1 : 0) << 1) + (checkOut.get() != null ? 1 : 0));
		if(checkIn.get() != null)
			oos.writeObject(checkIn.get());
		if(checkOut.get() != null)
			oos.writeObject(checkOut.get());
	}
	
	/**
	 * Get the employee.
	 *
	 * @return The employee.
	 */
	public Employee getEmployee()
	{
		return employeeProperty().get();
	}
	
	/**
	 * Get the employee property.
	 *
	 * @return The employee property.
	 */
	public SimpleObjectProperty<Employee> employeeProperty()
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
		employee = new SimpleObjectProperty<>((Employee) ois.readObject());
		date = new SimpleObjectProperty<>((LocalDate) ois.readObject());
		int infos = ois.readInt();
		if((infos & 0x02) == 0x02)
			checkIn = new RoundedLocalTimeProperty(getEmployee(), (LocalTime) ois.readObject());
		else
			checkIn = new RoundedLocalTimeProperty(getEmployee());
		if((infos & 0x01) == 0x01)
			checkOut = new RoundedLocalTimeProperty(getEmployee(), (LocalTime) ois.readObject());
		else
			checkOut = new RoundedLocalTimeProperty(getEmployee());
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof EmployeeCheck && getEmployee().equals(((EmployeeCheck) obj).getEmployee()) && getDate().equals(((EmployeeCheck) obj).getDate());
	}
	
	@Override
	public String toString()
	{
		return employee + " " + date + " IN: " + (checkIn == null ? "?" : checkIn) + " / OUT: " + (checkOut == null ? "?" : checkOut);
	}
	
	/**
	 * Get the time worked for this day.
	 *
	 * @return The time worked.
	 */
	public MinutesDuration getWorkedTime()
	{
		if(checkIn.get() == null || checkOut.get() == null)
			return MinutesDuration.ZERO;
		return MinutesDuration.seconds(checkOut.get().toSecondOfDay() - checkIn.get().toSecondOfDay());
	}
	
	/**
	 * Tells if a day is in progress (one check is present).
	 *
	 * @return True if one check is present, false else.
	 */
	public boolean isInProgress()
	{
		return (checkIn.get() != null) ^ (checkOut.get() != null);
	}
	
	/**
	 * Tell if the checks are in a valid order.
	 *
	 * @return True if valid, false else.
	 */
	public boolean isValidState()
	{
		return getCheckIn() == null || getCheckOut() == null || getCheckIn().isBefore(getCheckOut());
	}
	
	/**
	 * Get the check in time.
	 *
	 * @return The check in time.
	 */
	public LocalTime getCheckIn()
	{
		return checkInProperty().get();
	}
	
	/**
	 * Get the check out time.
	 *
	 * @return The check out time.
	 */
	public LocalTime getCheckOut()
	{
		return checkOutProperty().get();
	}
	
	/**
	 * Get the checkIn property.
	 *
	 * @return The checkIn property.
	 */
	public SimpleObjectProperty<LocalTime> checkInProperty()
	{
		return checkIn;
	}
	
	/**
	 * Get the checkOut property.
	 *
	 * @return The checkOut property.
	 */
	public SimpleObjectProperty<LocalTime> checkOutProperty()
	{
		return checkOut;
	}
}
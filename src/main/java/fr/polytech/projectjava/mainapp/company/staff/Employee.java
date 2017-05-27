package fr.polytech.projectjava.mainapp.company.staff;

import fr.polytech.projectjava.mainapp.company.Company;
import fr.polytech.projectjava.mainapp.company.departments.StandardDepartment;
import fr.polytech.projectjava.mainapp.company.staff.checking.CheckInOut;
import fr.polytech.projectjava.mainapp.company.staff.checking.EmployeeCheck;
import fr.polytech.projectjava.utils.Log;
import fr.polytech.projectjava.utils.jfx.MinutesDuration;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import static fr.polytech.projectjava.mainapp.company.staff.checking.CheckInOut.CheckType.IN;

/**
 * Represent an employee in the company.
 * Each one have a unique ID that is also their card ID.
 * <p>
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 23/03/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-03-23
 */
public class Employee extends Person implements Serializable
{
	protected final static LocalTime DEFAULT_ARRIVAL_TIME = Time.valueOf("08:30:00").toLocalTime();
	protected final static LocalTime DEFAULT_DEPARTURE_TIME = Time.valueOf("17:30:00").toLocalTime();
	private static final long serialVersionUID = -8611138931676775765L;
	protected static int NEXT_ID = 0;
	private int ID;
	private Company company;
	private ObservableList<EmployeeCheck> checks = FXCollections.observableArrayList();
	private ObservableList<DayOfWeek> workingDays = FXCollections.observableArrayList();
	private SimpleObjectProperty<MinutesDuration> lateDuration;
	private SimpleBooleanProperty isPresent;
	private SimpleObjectProperty<StandardDepartment> workingDepartment;
	private SimpleObjectProperty<LocalTime> arrivalTime;
	private SimpleObjectProperty<LocalTime> departureTime;
	
	/**
	 * Create an employee with his/her name.
	 *
	 * @param company   The company the employee is from.
	 * @param lastName  His/her last name.
	 * @param firstName His/her first name.
	 *
	 * @throws IllegalArgumentException If the arrival time is after the departure time.
	 */
	public Employee(Company company, String lastName, String firstName) throws IllegalArgumentException
	{
		this(company, lastName, firstName, DEFAULT_ARRIVAL_TIME, DEFAULT_DEPARTURE_TIME);
	}
	
	/**
	 * Create an employee with his/her name and its departure and arrival times.
	 *
	 * @param company       The company the employee is from.
	 * @param lastName      His/her last name.
	 * @param firstName     His/her first name.
	 * @param arrivalTime   The arrival time.
	 * @param departureTIme The departure time.
	 *
	 * @throws IllegalArgumentException If the arrival time is after the departure time.
	 */
	public Employee(Company company, String lastName, String firstName, LocalTime arrivalTime, LocalTime departureTIme) throws IllegalArgumentException
	{
		super(lastName, firstName);
		this.company = company;
		if(arrivalTime.isAfter(departureTIme))
			throw new IllegalArgumentException("Arrival time can't be after the departure time.");
		this.ID = NEXT_ID++;
		this.arrivalTime = new SimpleObjectProperty<>(arrivalTime);
		this.departureTime = new SimpleObjectProperty<>(departureTIme);
		this.lateDuration = new SimpleObjectProperty<>(MinutesDuration.ZERO);
		workingDepartment = new SimpleObjectProperty<>(null);
		isPresent = new SimpleBooleanProperty(false);
		workingDays.addAll(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Employee && ID == ((Employee) obj).getID();
	}
	
	/**
	 * Get the ID of the employee.
	 *
	 * @return Its ID.
	 */
	public int getID()
	{
		return ID;
	}
	
	/**
	 * Add a checking to this employee.
	 *
	 * @param checkInOut The checking to add.
	 */
	public void addCheckInOut(CheckInOut checkInOut)
	{
		boolean found = false;
		for(EmployeeCheck check : checks)
			if(check.isDateOf(checkInOut))
			{
				if(checkInOut.getCheckType() == IN)
					check.setIn(checkInOut.getTime());
				else
					check.setOut(checkInOut.getTime());
				
				found = true;
				break;
			}
		if(!found)
			addCheck(new EmployeeCheck(this, checkInOut));
		updateOvertime(null);
		updatePresence();
	}
	
	/**
	 * Add a check to the employee.
	 *
	 * @param check he check to add.
	 */
	public void addCheck(EmployeeCheck check)
	{
		if(!checks.contains(check))
		{
			checks.add(check);
			company.registerCheck(check);
		}
	}
	
	/**
	 * Get the number of minutes the employee done more.
	 *
	 * @param maxDate The maximum date to check for the times. If null, the current time will be used.
	 *
	 * @return The number of minutes overtime.
	 *
	 * @throws IllegalStateException If the checks are in an invalid state (more than 2 checks a day or 2 times the same type of check).
	 */
	public double updateOvertime(LocalDate maxDate) throws IllegalStateException
	{
		if(maxDate == null)
			maxDate = new Date(System.currentTimeMillis()).toLocalDate();
		
		Map<LocalDate, EmployeeCheck> checksByDate = checks.stream().collect(Collectors.toMap(EmployeeCheck::getDate, Function.identity()));
		LocalDate currentDate = checksByDate.keySet().stream().sorted(Comparator.naturalOrder()).findFirst().orElseGet(() -> new Date(System.currentTimeMillis()).toLocalDate());
		MinutesDuration overtime = MinutesDuration.ZERO;
		while(currentDate.compareTo(maxDate) <= 0)
		{
			if(checksByDate.containsKey(currentDate))
				overtime = overtime.add(checksByDate.get(currentDate).getWorkedTime()).substract(getWorkTimeForDay(currentDate.getDayOfWeek()));
			else
				overtime = overtime.substract(getWorkTimeForDay(currentDate.getDayOfWeek()));
			currentDate = currentDate.plusDays(1);
		}
		
		Log.info("New overtime for " + getFullName() + ": " + overtime);
		
		lateDuration.set(overtime);
		return overtime.getMinutes();
	}
	
	/**
	 * Update the presence of the employee based on the checks.
	 */
	public void updatePresence()
	{
		EmployeeCheck lastCheck = null;
		for(EmployeeCheck check : checks)
			if(lastCheck == null || lastCheck.getDate().isBefore(check.getDate()))
				lastCheck = check;
		if(lastCheck != null)
			isPresent.set(lastCheck.isInProgress());
	}
	
	/**
	 * Get the duration the employee should work for this day.
	 *
	 * @param dayOfWeek The day of the week concerned.
	 *
	 * @return The duration to work.
	 */
	private MinutesDuration getWorkTimeForDay(DayOfWeek dayOfWeek)
	{
		if(workingDays.contains(dayOfWeek))
			return MinutesDuration.seconds(getDepartureTime().toSecondOfDay() - getArrivalTime().toSecondOfDay());
		return MinutesDuration.ZERO;
	}
	
	/**
	 * Get the departure time of this employee.
	 *
	 * @return The departure time.
	 */
	public LocalTime getDepartureTime()
	{
		return departureTimeProperty().get();
	}
	
	/**
	 * Get the arrival time of this employee.
	 *
	 * @return The arrival time.
	 */
	public LocalTime getArrivalTime()
	{
		return arrivalTimeProperty().get();
	}
	
	/**
	 * Set the arrival time for this employee.
	 *
	 * @param arrivalTime The arrival time to set.
	 *
	 * @throws IllegalArgumentException If the arrival time is after the departure time.
	 */
	public void setArrivalTime(LocalTime arrivalTime) throws IllegalArgumentException
	{
		if(arrivalTime.isAfter(getDepartureTime()))
			throw new IllegalArgumentException("Arrival time can't be after the departure time.");
		this.arrivalTime.set(arrivalTime);
	}
	
	/**
	 * Get the departure time property.
	 *
	 * @return The departure time property.
	 */
	private SimpleObjectProperty<LocalTime> departureTimeProperty()
	{
		return departureTime;
	}
	
	/**
	 * Get the arrival time property.
	 *
	 * @return The arrival time property.
	 */
	private SimpleObjectProperty<LocalTime> arrivalTimeProperty()
	{
		return arrivalTime;
	}
	
	/**
	 * Set the departure time for this employee.
	 *
	 * @param departureTime The departure time to set.
	 *
	 * @throws IllegalArgumentException If the arrival time is after the departure time.
	 */
	public void setDepartureTime(LocalTime departureTime) throws IllegalArgumentException
	{
		if(getArrivalTime().isAfter(departureTime))
			throw new IllegalArgumentException("Arrival time can't be after the departure time.");
		this.departureTime.set(departureTime);
	}
	
	/**
	 * Add a working day for this employee.
	 *
	 * @param day The day to add.
	 */
	public void addWorkingDay(DayOfWeek day)
	{
		if(!workingDays.contains(day))
			workingDays.add(day);
	}
	
	/**
	 * Get the overtime property.
	 *
	 * @return The overtime property.
	 */
	public SimpleObjectProperty<MinutesDuration> lateDurationProperty()
	{
		return lateDuration;
	}
	
	/**
	 * Remove a check from this employee.
	 *
	 * @param check The check to remove.
	 */
	public void removeCheck(EmployeeCheck check)
	{
		checks.remove(check);
		company.unregisterCheck(check);
	}
	
	/**
	 * Tell if this employee have a check for a date.
	 *
	 * @param date The date to look for.
	 *
	 * @return True if the employee have a check on this date, false else.
	 */
	public boolean hasCheckForDate(LocalDate date)
	{
		for(EmployeeCheck check : checks)
			if(check.getDate().equals(date))
				return true;
		return false;
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
		oos.writeObject(company);
		oos.writeInt(getID());
		oos.writeObject(getWorkingDepartment());
		oos.writeObject(getArrivalTime());
		oos.writeObject(getDepartureTime());
		oos.writeInt(workingDays.size());
		for(DayOfWeek workingDay : workingDays)
			oos.writeObject(workingDay);
		oos.writeInt(checks.size());
		for(EmployeeCheck check : checks)
			oos.writeObject(check);
	}
	
	/**
	 * Get the department the employee is working in.
	 *
	 * @return The worker's department.
	 */
	public StandardDepartment getWorkingDepartment()
	{
		return workingDepartmentProperty().get();
	}
	
	/**
	 * Get the working department property.
	 *
	 * @return The working department property.
	 */
	public SimpleObjectProperty<StandardDepartment> workingDepartmentProperty()
	{
		return workingDepartment;
	}
	
	/**
	 * Set the working department for this employee.
	 *
	 * @param workingDepartment The department to affect him to.
	 */
	public void setWorkingDepartment(StandardDepartment workingDepartment)
	{
		this.workingDepartment.set(workingDepartment);
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
		company = (Company) ois.readObject();
		ID = ois.readInt();
		NEXT_ID = Math.max(ID, NEXT_ID);
		workingDepartment = new SimpleObjectProperty<>((StandardDepartment) ois.readObject());
		arrivalTime = new SimpleObjectProperty<>((LocalTime) ois.readObject());
		departureTime = new SimpleObjectProperty<>((LocalTime) ois.readObject());
		
		workingDays = FXCollections.observableArrayList();
		int wkdCount = ois.readInt();
		for(int i = 0; i < wkdCount; i++)
			workingDays.add((DayOfWeek) ois.readObject());
		
		checks = FXCollections.observableArrayList();
		int chkCount = ois.readInt();
		for(int i = 0; i < chkCount; i++)
			checks.add((EmployeeCheck) ois.readObject());
		
		lateDuration = new SimpleObjectProperty<>(MinutesDuration.ZERO);
		isPresent = new SimpleBooleanProperty(false);
		
		updateOvertime(null);
		updatePresence();
	}
	
	/**
	 * Get the presence property.
	 *
	 * @return The presence property.
	 */
	public SimpleBooleanProperty isPresentProperty()
	{
		return isPresent;
	}
	
	/**
	 * Get the list of checking the employee did.
	 *
	 * @return A list of the checking.
	 */
	public ObservableList<EmployeeCheck> getChecks()
	{
		return checks;
	}
}
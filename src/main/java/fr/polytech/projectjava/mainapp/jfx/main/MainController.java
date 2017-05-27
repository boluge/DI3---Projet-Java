package fr.polytech.projectjava.mainapp.jfx.main;

import fr.polytech.projectjava.mainapp.company.Company;
import fr.polytech.projectjava.mainapp.company.departments.StandardDepartment;
import fr.polytech.projectjava.mainapp.company.staff.Employee;
import fr.polytech.projectjava.mainapp.company.staff.Manager;
import fr.polytech.projectjava.mainapp.company.staff.checking.CheckInOut;
import fr.polytech.projectjava.mainapp.company.staff.checking.EmployeeCheck;
import fr.polytech.projectjava.mainapp.jfx.main.check.CheckList;
import fr.polytech.projectjava.mainapp.jfx.main.check.create.CheckCreateDialog;
import fr.polytech.projectjava.mainapp.jfx.main.company.create.CompanyCreateDialog;
import fr.polytech.projectjava.mainapp.jfx.main.department.DepartmentList;
import fr.polytech.projectjava.mainapp.socket.CheckingServer;
import fr.polytech.projectjava.utils.Configuration;
import fr.polytech.projectjava.utils.Log;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import java.io.*;
import java.util.Optional;

/**
 * Controller for the main window.
 * <p>
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 27/04/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-04-27
 */
public class MainController
{
	private final MainApplication parent;
	private final CheckingServer socketReceiver;
	private Company company;
	
	/**
	 * Constructor.
	 *
	 * @param mainApplication The main window.
	 *
	 * @throws IOException If the socket failed to be opened.
	 */
	public MainController(MainApplication mainApplication) throws IOException
	{
		parent = mainApplication;
		socketReceiver = new CheckingServer(this);
		new Thread(socketReceiver).start();
	}
	
	/**
	 * Used when the application closes. Stop the socket server and save datas.
	 *
	 * @param windowEvent The even of the close request.
	 */
	public void close(WindowEvent windowEvent)
	{
		socketReceiver.stop();
		saveDatas();
	}
	
	/**
	 * Save the current company.
	 */
	public void saveDatas()
	{
		if(company != null)
		{
			try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(Configuration.getString("mainSaveFile")))))
			{
				oos.writeObject(company);
			}
			catch(IOException e)
			{
				Log.error("Failed to save company", e);
			}
		}
	}
	
	/**
	 * List the employees of the company.
	 *
	 * @return The employees.
	 */
	public ObservableList<Employee> listEmployees()
	{
		return getCompany().getEmployees();
	}
	
	/**
	 * Get the current loaded company.
	 *
	 * @return The loaded company.
	 */
	public Company getCompany()
	{
		return company;
	}
	
	/**
	 * Add a check.
	 *
	 * @param employeeID The employee ID.
	 * @param check      The check to add.
	 *
	 * @return True if the check was added, false else.
	 */
	public boolean addChecking(int employeeID, CheckInOut check)
	{
		Optional<Employee> employee = getCompany().getEmployee(employeeID);
		if(employee.isPresent())
		{
			employee.get().addCheckInOut(check);
			return true;
		}
		return false;
	}
	
	/**
	 * Handle a click on an employee in the list.
	 *
	 * @param event The click event.
	 */
	public void employeeClick(MouseEvent event)
	{
		/*if(event.getClickCount() >= 2 && event.getSource() instanceof TableRow)
		{
			TableRow source = (TableRow) event.getSource();
			if(source.getItem() instanceof Employee)
			{
				Employee employee = (Employee) source.getItem();
				if(event.getButton() == MouseButton.PRIMARY)
				{
					((TableRow) event.getSource()).getScene();
					EmployeeDialog dialog = new EmployeeDialog(employee);
					dialog.initOwner(((TableRow) event.getSource()).getScene().getWindow());
					dialog.initModality(Modality.APPLICATION_MODAL);
					dialog.showAndWait();
				}
			}
		}*///TODO
	}
	
	/**
	 * Get an employee by its ID.
	 *
	 * @param ID The employee ID.
	 *
	 * @return An optional of the employee.
	 */
	public Optional<Employee> getEmployeeByID(int ID)
	{
		return getCompany().getEmployee(ID);
	}
	
	/**
	 * Builds a new company.
	 *
	 * @return The created company.
	 */
	private Company buildNewCompany()
	{
		CompanyCreateDialog dialog = new CompanyCreateDialog();
		dialog.initOwner(parent.getStage());
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.showAndWait();
		return dialog.getResult();
	}
	
	/**
	 * Load a company into the view.
	 *
	 * @return True if a company was loaded, false else.
	 */
	public boolean loadCompany()
	{
		Optional<Company> companyOptional = loadLastCompany();
		company = companyOptional.orElseGet(this::buildNewCompany);
		if(company == null)
		{
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("No company loaded");
			alert.setHeaderText("No company loaded");
			alert.setContentText("A company need to be loaded or created for the application to work.");
			alert.showAndWait();
			parent.getStage().close();
			Platform.exit();
			return false;
		}
		SimpleStringProperty employeeCount = new SimpleStringProperty("" + getCompany().getEmployees().size());
		SimpleStringProperty departmentCount = new SimpleStringProperty("" + getCompany().getDepartements().size());
		company.getEmployees().addListener((InvalidationListener) observable -> employeeCount.set("" + company.getEmployees().size()));
		company.getDepartements().addListener((InvalidationListener) observable -> departmentCount.set("" + company.getDepartements().size()));
		
		parent.getMainTab().getCompanyNameTextProperty().bind(company.nameProperty());
		parent.getMainTab().getBossNameTextProperty().bind(company.getBoss().fullNameProperty());
		parent.getMainTab().getEmployeeCountTextProperty().bind(employeeCount);
		parent.getMainTab().getDepartmentCountTextProperty().bind(departmentCount);
		parent.getEmployeeTab().getList().setList(company.getEmployees());
		parent.getEmployeeTab().getDepartmentFilter().setItems(company.getDepartements());
		parent.getDepartmentTab().getList().setList(company.getDepartements());
		parent.getCheckTab().getList().setList(company.getChecks());
		parent.getCheckTab().getDepartmentFilter().setItems(company.getDepartements());
		parent.getCheckTab().getEmployeeFilter().setItems(company.getEmployees());
		return true;
	}
	
	/**
	 * Load the last company.
	 *
	 * @return The loaded company.
	 */
	private Optional<Company> loadLastCompany()
	{
		File f = new File(Configuration.getString("mainSaveFile"));
		if(f.exists() && f.isFile())
		{
			Company company = null;
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f)))
			{
				company = (Company) ois.readObject();
			}
			catch(IOException | ClassNotFoundException | ClassCastException e)
			{
				Log.warning("Failed to load last company", e);
			}
			return Optional.ofNullable(company);
		}
		return Optional.empty();
	}
	
	/**
	 * Bring the popup when an employee want to be added.
	 *
	 * @param event The click event.
	 */
	public void addEmployee(ActionEvent event)
	{
		//TODO
	}
	
	/**
	 * Bring the popup when a company want to be added.
	 *
	 * @param event The click event.
	 */
	public void addDepartment(ActionEvent event)
	{
		//TODO
	}
	
	/**
	 * Remove a department from the list.
	 * This is done only if the department is empty.
	 *
	 * @param evt             The click event.
	 * @param departmentsList The department list.
	 */
	public void removeDepartment(ActionEvent evt, DepartmentList departmentsList)
	{
		if(departmentsList.getSelectionModel().getSelectedItem() != null)
		{
			StandardDepartment dpt = departmentsList.getSelectionModel().getSelectedItem();
			if(dpt.getEmployees().size() == 0)
				company.removeDepartment(dpt);
			else
			{
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Department not empty");
				alert.setHeaderText("Department not empty");
				alert.setContentText("The department must have no employee before being deleted");
				alert.showAndWait();
			}
		}
	}
	
	/**
	 * Bring the popup when a check want to be added.
	 *
	 * @param event The click event.
	 */
	public void addCheck(ActionEvent event)
	{
		CheckCreateDialog dialog = new CheckCreateDialog(company.getEmployees());
		dialog.initOwner(((Button) event.getSource()).getScene().getWindow());
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.showAndWait();
		EmployeeCheck result = dialog.getResult();
		if(result != null)
			result.getEmployee().addCheck(result);
	}
	
	/**
	 * Remove a check from the list.
	 *
	 * @param evt       The click event.
	 * @param checkList The check list.
	 */
	public void removeCheck(ActionEvent evt, CheckList checkList)
	{
		if(checkList.getSelectionModel().getSelectedItem() != null)
		{
			EmployeeCheck check = checkList.getSelectionModel().getSelectedItem();
			Log.info("Removing check " + check);
			check.getEmployee().removeCheck(check);
		}
	}
	
	/**
	 * Called when the manager of a department is modified.
	 *
	 * @param event The change event.
	 */
	public void managerChanged(TableColumn.CellEditEvent<StandardDepartment, Manager> event)
	{
		event.getRowValue().setLeader(event.getNewValue());
	}
}
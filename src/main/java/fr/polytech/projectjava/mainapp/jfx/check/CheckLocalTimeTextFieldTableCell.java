package fr.polytech.projectjava.mainapp.jfx.check;

import fr.polytech.projectjava.mainapp.company.staff.checking.EmployeeCheck;
import fr.polytech.projectjava.utils.jfx.LocalTimeTextFieldTableCell;
import java.time.LocalTime;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 28/05/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-05-28
 */
public class CheckLocalTimeTextFieldTableCell extends LocalTimeTextFieldTableCell<EmployeeCheck>
{
	@Override
	public void updateItem(LocalTime item, boolean empty)
	{
		super.updateItem(item, empty);
		if(!empty && getTableRow().getItem() != null && !((EmployeeCheck) getTableRow().getItem()).isValidState())
			getTableRow().setStyle("-fx-background-color: #FF0000;");
		else
			getTableRow().setStyle(null);
	}
}

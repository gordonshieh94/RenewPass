package ca.alexland.renewpass.utils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.gistlabs.mechanize.document.html.HtmlDocument;
import com.gistlabs.mechanize.document.html.form.Checkbox;
import com.gistlabs.mechanize.document.html.form.Form;
import com.gistlabs.mechanize.document.html.form.Select;
import com.gistlabs.mechanize.exceptions.MechanizeException;
import com.gistlabs.mechanize.impl.MechanizeAgent;

import java.util.List;

import ca.alexland.renewpass.MainActivity;
import ca.alexland.renewpass.SettingsActivity;
import ca.alexland.renewpass.exceptions.NothingToRenewException;
import ca.alexland.renewpass.exceptions.SchoolAuthenticationFailedException;
import ca.alexland.renewpass.exceptions.SchoolNotFoundException;
import ca.alexland.renewpass.model.Callback;
import ca.alexland.renewpass.model.Status;
import ca.alexland.renewpass.schools.School;
import ca.alexland.renewpass.schools.SimonFraserUniversity;
import ca.alexland.renewpass.views.LoadingFloatingActionButton;

/**
 * Created by AlexLand on 2015-12-28.
 */
public class UPassLoader {
    private Callback callback;
    private final String UPASS_SITE_URL = "http://upassbc.translink.ca";

    public static void renewUPass(Context context, Callback callback) {
        UPassLoader mService = new UPassLoader();
        School school = new SimonFraserUniversity();
        PreferenceHelper preferenceHelper = new PreferenceHelper(context);
        String username = preferenceHelper.getUsername();
        String password = preferenceHelper.getPassword();
        boolean doRenew = true;
        mService.startRenew(doRenew, school, username, password, callback);
    }

    public static void checkUPassAvailable(Context context, Callback callback) {
        UPassLoader mService = new UPassLoader();
        School school = new SimonFraserUniversity();
        PreferenceHelper preferenceHelper = new PreferenceHelper(context);
        String username = preferenceHelper.getUsername();
        String password = preferenceHelper.getPassword();
        boolean doRenew = false;
        mService.startRenew(doRenew, school, username, password, callback);
    }

    private void startRenew(boolean doRenew, School school, String username, String password, Callback callback) {
        this.callback = callback;
        new RenewTask(school, username, password).execute(doRenew);
    }

    private class RenewTask extends AsyncTask<Boolean, Void, Status> {
        School school;
        String username;
        String password;

        public RenewTask(School school, String username, String password) {
            this.school = school;
            this.username = username;
            this.password = password;
        }

        @Override
        protected ca.alexland.renewpass.model.Status doInBackground(Boolean... params) {
            Boolean doRenew = params[0];
            ca.alexland.renewpass.model.Status returnStatus;
            try {
                HtmlDocument authPage = selectSchool(UPASS_SITE_URL, school.getID());
                HtmlDocument upassPage = authorizeAccount(authPage);
                Form requestForm = checkUpass(upassPage);
                if (doRenew) {
                    requestUpass(requestForm);
                    returnStatus = new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.RENEW_SUCCESSFUL, true);
                }
                else {
                    returnStatus = new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.UPASS_AVAILABLE, true);
                }
            }
            catch(SchoolNotFoundException e) {
                return new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.SCHOOL_NOT_FOUND, false);
            }
            catch(SchoolAuthenticationFailedException e) {
                return new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.AUTHENTICATION_ERROR, false);
            }
            catch(NothingToRenewException e) {
                return new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.NOTHING_TO_RENEW, true);
            }
            catch(MechanizeException e) {
                return new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.NETWORK_ERROR, false);
            }
            catch(Exception e) {
                return new ca.alexland.renewpass.model.Status(ca.alexland.renewpass.model.Status.UNKNOWN_ERROR, false);
            }

            return returnStatus;
        }

        private HtmlDocument selectSchool(String siteURL, String schoolId) throws SchoolNotFoundException {
            MechanizeAgent agent = new MechanizeAgent();
            HtmlDocument page = agent.get(siteURL);
            Form schoolSelectionForm = page.forms().get(0);
            Select schoolDropdown = (Select) schoolSelectionForm.get("PsiId");
            List<Select.Option> schools = schoolDropdown.getOptions();
            Select.Option schoolOption = null;
            for(Select.Option school : schools) {
                if (school.getValue().equals(schoolId)) {
                    schoolOption = school;
                }
            }
            if (schoolOption != null) {
                schoolOption.setSelected(true);
            }
            else {
                throw new SchoolNotFoundException();
            }
            return schoolSelectionForm.submit();
        }

        private HtmlDocument authorizeAccount(HtmlDocument authPage) throws SchoolAuthenticationFailedException {
            return this.school.login(authPage, this.username, this.password);
        }

        private Form checkUpass(HtmlDocument upassPage) throws NothingToRenewException {
            Form requestForm = upassPage.form("form-request");
            Checkbox requestCheckbox = requestForm.findCheckbox("Selected");
            if (requestCheckbox != null) {
                return requestForm;
            }
            else {
                throw new NothingToRenewException();
            }
        }

        private void requestUpass(Form requestForm) {
            Checkbox requestCheckbox = requestForm.findCheckbox("Selected");
            requestCheckbox.check();
            HtmlDocument resultPage = requestForm.submit();
            // TODO: Check table for successful request
        }

        @Override
        protected void onPostExecute(ca.alexland.renewpass.model.Status result) {
            callback.onUPassLoaded(result);
        }
    }
}

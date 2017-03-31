package com.ystrazhko.git.ui.javafx.controllers;

import com.ystrazhko.git.entities.Group;
import com.ystrazhko.git.jgit.JGit;
import com.ystrazhko.git.services.GroupsUserService;
import com.ystrazhko.git.services.LoginService;
import com.ystrazhko.git.services.ServiceProvider;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.File;
import java.util.List;

public class CloningGroupsWindowController {
    private static final String FOLDER_CHOOSER_DIALOG = "Destination folder";

    private LoginService _loginService =
            (LoginService) ServiceProvider.getInstance().getService(LoginService.class.getName());

    private GroupsUserService _groupsService =
            (GroupsUserService) ServiceProvider.getInstance().getService(GroupsUserService.class.getName());

    @FXML
    private TextField folderPath;

    @FXML
    private ListView<Group> projectsList;

    @FXML
    public Button okButton;

    @FXML
    public void initialize() {
        List<Group> userGroups = (List<Group>) _groupsService.getGroups(_loginService.getCurrentUser());
        ObservableList<Group> myObservableList = FXCollections.observableList(userGroups);

        configureListView(projectsList);
        projectsList.setItems(myObservableList);

        BooleanBinding booleanBinding =
                projectsList.getSelectionModel().selectedItemProperty().isNull().or(
                        folderPath.textProperty().isEqualTo(""));

        okButton.disableProperty().bind(booleanBinding);
    }

    @FXML
    public void onBrowseButton(ActionEvent actionEvent) throws Exception {
        Node source = (Node) actionEvent.getSource();
        Window theStage = source.getScene().getWindow();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(FOLDER_CHOOSER_DIALOG);
        File selectedDirectory = chooser.showDialog(theStage);
        if (selectedDirectory != null) {
            folderPath.setText(selectedDirectory.getCanonicalPath());
        }
    }

    @FXML
    public void onOkButton(ActionEvent actionEvent) throws Exception {
        String destinationPath = folderPath.getText();

        List<Group> selectedGroups = projectsList.getSelectionModel().getSelectedItems();
        for (Group groupItem : selectedGroups) {
            JGit.getInstance().clone(groupItem, destinationPath);
        }
    }

    @FXML
    public void onCancelButton(ActionEvent actionEvent) throws Exception {

    }

    private void configureListView(ListView listView) {
        //config displayable string
        listView.setCellFactory(new Callback<ListView<Group>, ListCell<Group>>() {
            @Override
            public ListCell<Group> call(ListView<Group> p) {
                ListCell<Group> cell = new ListCell<Group>() {
                    @Override
                    protected void updateItem(Group item, boolean bln) {
                        super.updateItem(item, bln);
                        if (item != null) {
                            String itemText = item.getName() + " (@" + item.getPath() + ") ";
                            setText(itemText);
                        }
                    }
                };

                return cell;
            }
        });

        //setup selection
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.addEventFilter(MouseEvent.MOUSE_PRESSED, evt -> {
            Node node = evt.getPickResult().getIntersectedNode();

            while (node != null && node != listView && !(node instanceof ListCell)) {
                node = node.getParent();
            }

            if (node instanceof ListCell) {
                evt.consume();

                ListCell cell = (ListCell) node;
                ListView lv = cell.getListView();

                lv.requestFocus();

                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (cell.isSelected()) {
                        lv.getSelectionModel().clearSelection(index);
                    } else {
                        lv.getSelectionModel().select(index);
                    }
                }
            }
        });
    }
}

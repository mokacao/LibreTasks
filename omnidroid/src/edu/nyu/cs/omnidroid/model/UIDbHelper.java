/*******************************************************************************
 * Copyright 2009 OmniDroid - http://code.google.com/p/omnidroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.nyu.cs.omnidroid.model;

import static edu.nyu.cs.omnidroid.model.CursorHelper.getIntFromCursor;
import static edu.nyu.cs.omnidroid.model.CursorHelper.getStringFromCursor;
import static edu.nyu.cs.omnidroid.model.CursorHelper.getLongFromCursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import edu.nyu.cs.omnidroid.R;
import edu.nyu.cs.omnidroid.core.datatypes.DataType;
import edu.nyu.cs.omnidroid.core.datatypes.FactoryDataType;
import edu.nyu.cs.omnidroid.model.db.DataFilterDbAdapter;
import edu.nyu.cs.omnidroid.model.db.DataTypeDbAdapter;
import edu.nyu.cs.omnidroid.model.db.DbHelper;
import edu.nyu.cs.omnidroid.model.db.RegisteredActionDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RegisteredActionParameterDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RegisteredAppDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RegisteredEventAttributeDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RegisteredEventDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleActionDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleActionParameterDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleDbAdapter;
import edu.nyu.cs.omnidroid.model.db.RuleFilterDbAdapter;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelAction;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelApplication;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelAttribute;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelEvent;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelFilter;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelParameter;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelRuleAction;
import edu.nyu.cs.omnidroid.ui.simple.model.ModelRuleFilter;
import edu.nyu.cs.omnidroid.ui.simple.model.Rule;
import edu.nyu.cs.omnidroid.ui.simple.model.RuleNode;

/**
 * This class serves as an access layer of the database for Omnidroid's UI data model 
 * representation.
 */
public class UIDbHelper {

  private DbHelper omnidroidDbHelper;
  private SQLiteDatabase database;
  
  private DataTypeDbAdapter dataTypeDbAdapter;
  private DataFilterDbAdapter dataFilterDbAdapter;
  private RegisteredAppDbAdapter registeredAppDbAdapter;
  private RegisteredEventDbAdapter registeredEventDbAdapter;
  private RegisteredActionDbAdapter registeredActionDbAdapter;
  private RegisteredEventAttributeDbAdapter registeredEventAttributeDbAdapter;
  private RegisteredActionParameterDbAdapter registeredActionParameterDbAdapter;
  private RuleFilterDbAdapter ruleFilterDbAdapter;
  private RuleActionDbAdapter ruleActionDbAdapter;
  private RuleActionParameterDbAdapter ruleActionParameterDbAdapter;
  private RuleDbAdapter ruleDbAdapter;

  // Hash maps for storing cached data for quick lookup
  private HashMap<Integer, String> dataTypeNames;
  private HashMap<Integer, String> dataTypeClassNames;
  private HashMap<Integer, String> dataFilterNames;
  private HashMap<Integer, ModelApplication> applications;
  private HashMap<Integer, ModelEvent> events;
  private HashMap<Integer, ModelAction> actions;
  private HashMap<Integer, ModelAttribute> attributes;
  private HashMap<Integer, ModelParameter> parameters;
  
  // This flag marks whether this helper is closed
  private boolean isClosed = false;

  public UIDbHelper(Context context) {
    omnidroidDbHelper = new DbHelper(context);
    database = omnidroidDbHelper.getWritableDatabase();
    
    // Initialize db adapters
    dataTypeDbAdapter = new DataTypeDbAdapter(database);
    dataFilterDbAdapter = new DataFilterDbAdapter(database);
    registeredAppDbAdapter = new RegisteredAppDbAdapter(database);
    registeredEventDbAdapter = new RegisteredEventDbAdapter(database);
    registeredActionDbAdapter = new RegisteredActionDbAdapter(database);
    registeredEventAttributeDbAdapter = new RegisteredEventAttributeDbAdapter(database);
    registeredActionParameterDbAdapter = new RegisteredActionParameterDbAdapter(database);
    ruleFilterDbAdapter = new RuleFilterDbAdapter(database);
    ruleActionDbAdapter = new RuleActionDbAdapter(database);
    ruleActionParameterDbAdapter = new RuleActionParameterDbAdapter(database);
    ruleDbAdapter = new RuleDbAdapter(database);

    // Initialize db cache
    dataTypeNames = new HashMap<Integer, String>();
    dataTypeClassNames = new HashMap<Integer, String>();
    dataFilterNames = new HashMap<Integer, String>();
    applications = new HashMap<Integer, ModelApplication>();
    events = new HashMap<Integer, ModelEvent>();
    actions = new HashMap<Integer, ModelAction>();
    attributes = new HashMap<Integer, ModelAttribute>();
    parameters = new HashMap<Integer, ModelParameter>();

    // Load db cache maps
    loadDbCache();
  }

  /**
   * Close the UIDbHelper. UI needs to call this method when it is done with the database 
   * connection. UIDbHelper is not usable after calling this method. 
   */
  public void close() {
    isClosed = true;
    omnidroidDbHelper.close();
    database.close();
  }

  /**
   * Load cached data into hash maps
   */
  private void loadDbCache() {

    // TODO(ehotou) Consider lazy initialization for some of these stuff.
    
    // Load DataTypes
    Cursor cursor = dataTypeDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      dataTypeNames.put(getIntFromCursor(cursor, DataTypeDbAdapter.KEY_DATATYPEID),
          getStringFromCursor(cursor, DataTypeDbAdapter.KEY_DATATYPENAME));
      dataTypeClassNames.put(getIntFromCursor(cursor, DataTypeDbAdapter.KEY_DATATYPEID), 
          getStringFromCursor(cursor, DataTypeDbAdapter.KEY_DATATYPECLASSNAME));
    }
    cursor.close();

    // Load Filters
    cursor = dataFilterDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      dataFilterNames.put(getIntFromCursor(cursor, DataFilterDbAdapter.KEY_DATAFILTERID),
          getStringFromCursor(cursor, DataFilterDbAdapter.KEY_DATAFILTERNAME));
    }
    cursor.close();

    // Load applications
    cursor = registeredAppDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      ModelApplication application = new ModelApplication(
            getStringFromCursor(cursor, RegisteredAppDbAdapter.KEY_APPNAME), 
            "", //TODO(ehotou) After implementing desc for app, load it here
            R.drawable.icon_application_unknown, 
            getIntFromCursor(cursor, RegisteredAppDbAdapter.KEY_APPID));
      applications.put(application.getDatabaseId(), application);
    }
    cursor.close();

    // Load Events
    cursor = registeredEventDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      ModelEvent event = new ModelEvent(
          getIntFromCursor(cursor, RegisteredEventDbAdapter.KEY_EVENTID),
          getStringFromCursor(cursor, RegisteredEventDbAdapter.KEY_EVENTNAME), 
          "", //TODO(ehotou) After implementing desc for event, load it here
          R.drawable.icon_event_unknown);
      events.put(event.getDatabaseId(), event);
    }
    cursor.close();
    
    // Load Event Attributes
    cursor = registeredEventAttributeDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      ModelAttribute attribute = new ModelAttribute(
          getIntFromCursor(cursor, RegisteredEventAttributeDbAdapter.KEY_EVENTATTRIBUTEID), 
          getIntFromCursor(cursor, RegisteredEventAttributeDbAdapter.KEY_EVENTID), 
          getIntFromCursor(cursor, RegisteredEventAttributeDbAdapter.KEY_DATATYPEID), 
          getStringFromCursor(cursor, RegisteredEventAttributeDbAdapter.KEY_EVENTATTRIBUTENAME), 
          "", //TODO(ehotou) After implementing desc for attribute, load it here
          R.drawable.icon_attribute_unknown);
      
      attributes.put(attribute.getDatabaseId(), attribute);
    }
    cursor.close();

    // Load Action Parameters  
    cursor = registeredActionParameterDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      ModelParameter parameter = new ModelParameter(
          getIntFromCursor(cursor, RegisteredActionParameterDbAdapter.KEY_ACTIONPARAMETERID),
          getIntFromCursor(cursor, RegisteredActionParameterDbAdapter.KEY_ACTIONID),
          getIntFromCursor(cursor, RegisteredActionParameterDbAdapter.KEY_DATATYPEID),
          getStringFromCursor(cursor, RegisteredActionParameterDbAdapter.KEY_ACTIONPARAMETERNAME),
          "" //TODO(ehotou) After implementing desc for parameter, load it here
          );
      
      parameters.put(parameter.getDatabaseId(), parameter);
    }
    cursor.close();
    
    // Load Actions
    cursor = registeredActionDbAdapter.fetchAll();
    while (cursor.moveToNext()) {
      ModelApplication application = applications.get(
          getIntFromCursor(cursor, RegisteredActionDbAdapter.KEY_APPID));
      
      int actionID = getIntFromCursor(cursor, RegisteredActionDbAdapter.KEY_ACTIONID);
      
      // Load parameters for each action
      ArrayList<ModelParameter> parameterList = new ArrayList<ModelParameter>();
      for (ModelParameter parameter : parameters.values()) {
        if (parameter.getForeignKeyActionId() == actionID) {
          parameterList.add(parameter);
        }
      }

      ModelAction action = new ModelAction(
          getStringFromCursor(cursor, RegisteredActionDbAdapter.KEY_ACTIONNAME), 
          "", //TODO(ehotou) After implementing desc for action, load it here
          R.drawable.icon_action_unknown, actionID, application, parameterList);
      
      actions.put(actionID, action);
    }
    cursor.close();
  }

  /**
   * Create a dataType object with a type matches dataTypeID, containing specific data.
   * 
   * @param dataTypeID
   *          is id of the dataType to be created
   *          
   * @param data
   *          is the content of the data within the dataType object
   *          
   * @return a dataType object
   */
  private DataType getDataType(int dataTypeID, String data) {
    String dataTypeClassName = dataTypeClassNames.get(dataTypeID);
    return FactoryDataType.createObject(dataTypeClassName, data);
  }

  /**
   * @return all applications as an ArrayList
   * @throws IllegalStateException if this helper is closed
   */
  public ArrayList<ModelApplication> getAllApplications() {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    ArrayList<ModelApplication> applicationList = new ArrayList<ModelApplication>(
        applications.size());
    applicationList.addAll(applications.values());
    return applicationList;
  }

  /**
   * @return all events as an ArrayList
   * @throws IllegalStateException if this helper is closed
   */
  public ArrayList<ModelEvent> getAllEvents() {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    ArrayList<ModelEvent> eventList = new ArrayList<ModelEvent>(events.size());
    eventList.addAll(events.values());
    return eventList;
  }

  /**
   * @param application
   *          is a ModelApplication object
   *          
   * @return all actions associated with one application as an ArrayList
   * @throws IllegalStateException if this helper is closed
   */
  public ArrayList<ModelAction> getActionsForApplication(ModelApplication application) {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    ArrayList<ModelAction> actionList = new ArrayList<ModelAction>(actions.size());
    for (ModelAction action : actions.values()) {
      if (action.getApplication().getDatabaseId() == application.getDatabaseId()) {
        actionList.add(action);
      }
    }
    return actionList;
  }

  /**
   * @param event
   *          is a ModelEvent object
   *          
   * @return all attributes associated with one event as an ArrayList
   * @throws IllegalStateException if this helper is closed
   */
  public ArrayList<ModelAttribute> getAttributesForEvent(ModelEvent event) {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    ArrayList<ModelAttribute> attributesList = new ArrayList<ModelAttribute>(attributes.size());
    for (ModelAttribute attribute : attributes.values()) {
      if(attribute.getForeignKeyEventId() == event.getDatabaseId()) {
        attributesList.add(attribute);
      }
    }
    return attributesList;
  }

  /**
   * @param attribute
   *          is a ModelAttribute object
   *          
   * @return all filters associated with one attribute as an ArrayList
   * @throws IllegalStateException if this helper is closed
   */
  public ArrayList<ModelFilter> getFiltersForAttribute(ModelAttribute attribute) {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    
    // Fetch all filter that filters on this attribute's dataType, set filterName to null, 
    // set compareWithDatatypeID to null
    Cursor cursor = dataFilterDbAdapter.fetchAll(null, Long.valueOf(attribute.getDatatype()), null);
    ArrayList<ModelFilter> filterList = new ArrayList<ModelFilter>(cursor.getCount());
    
    while (cursor.moveToNext()) {
      int filterID = getIntFromCursor(cursor, DataFilterDbAdapter.KEY_DATAFILTERID);
      String filterName = dataFilterNames.get(filterID);

      filterList.add(new ModelFilter(filterName, 
              "" // TODO(ehotou) After implementing desc for filter, load it here 
              , R.drawable.icon_filter_unknown, filterID,
              attribute));
    }
    cursor.close();
    return filterList;
  }

  /**
   * @return all Rule objects (sparse version just contains id, name) as an ArrayList
   * @throws IllegalStateException if this helper is closed
   */
  public ArrayList<Rule> getRules() {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    
    Cursor cursor = ruleDbAdapter.fetchAll();
    ArrayList<Rule> ruleList = new ArrayList<Rule>(cursor.getCount());
    
    while (cursor.moveToNext()) {
      Rule rule = new Rule(getIntFromCursor(cursor, RuleDbAdapter.KEY_RULEID));
      rule.setName(getStringFromCursor(cursor, RuleDbAdapter.KEY_RULENAME));
      // TODO(ehotou) need to set rule active flag when it is added into rule object
      
      ruleList.add(rule);
    }
    cursor.close();
    return ruleList;
  }

  /**
   * @param databaseId
   *          is the rule id
   *          
   * @return a fully loaded Rule object with databaseId
   * @throws IllegalStateException if this helper is closed
   */
  public Rule loadRule(int databaseId) {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    
    Cursor cursorRule = ruleDbAdapter.fetch(Long.valueOf(databaseId));

    Rule rule = new Rule(databaseId);

    // Fetch and set the root event.
    ModelEvent event = events.get(getIntFromCursor(cursorRule, RuleDbAdapter.KEY_EVENTID));
    rule.setRootEvent(event);

    // Add all filters for this rule to the root node in a tree format.
    addFiltersToRuleNode(databaseId, rule.getRootNode());

    // Add all actions for this rule
    ArrayList<ModelRuleAction> actionList = getActionsForRule(databaseId);
    for (ModelRuleAction action : actionList) {
      rule.getRootNode().addChild(action);
    }
    
    cursorRule.close();

    return rule;
  }

  /**
   * Adds all filters to a root node in tree form.
   * 
   * TODO(ehotou) consider moving part of this method which constructs the tree structure to some
   * util/builder method within Rule class
   * 
   * @param ruleId
   *          is id of the rule record to be loaded
   * 
   * @param is
   *          the event node at the root of the rule structure
   */
  private void addFiltersToRuleNode(int ruleId, RuleNode rootEvent) {

    // Map<filterId, filterParentId>, for all filters of the rule.
    HashMap<Integer, Integer> parentIds = new HashMap<Integer, Integer>();
    
    // All filters keyed by filterId for quick lookup below.
    HashMap<Integer, ModelRuleFilter> filtersUnlinked = new HashMap<Integer, ModelRuleFilter>();

    // Fetch all ruleFilter associated with this rule, set other parameters to be null
    Cursor cursorRuleFilters = ruleFilterDbAdapter.fetchAll(Long.valueOf(ruleId), null, null, null,
        null, null);
    
    while (cursorRuleFilters.moveToNext()) {

      // Get attribute that this ruleFilter associated with
      ModelAttribute attribute = attributes.get(
          getIntFromCursor(cursorRuleFilters, RuleFilterDbAdapter.KEY_EVENTATTRIBUTEID));

      // Load the user defined data within this rule filter
      String filterInputFromUser = getStringFromCursor(
          cursorRuleFilters, RuleFilterDbAdapter.KEY_RULEFILTERDATA);

      // Construct a data type object
      DataType filterData = getDataType(attribute.getDatatype(), filterInputFromUser);

      // For filters, first load up the model filter, then supply it to
      // the model rule filter instance.
      int filterID = getIntFromCursor(cursorRuleFilters, RuleFilterDbAdapter.KEY_DATAFILTERID);
      String filterName = dataFilterNames.get(filterID);
      
      ModelFilter modelFilter = new ModelFilter(filterName, 
          "", // TODO(ehotou) After implementing desc for filter, load it here.
          R.drawable.icon_filter_unknown, filterID, attribute);

      ModelRuleFilter filter = new ModelRuleFilter(
          getIntFromCursor(cursorRuleFilters, RuleFilterDbAdapter.KEY_RULEFILTERID), 
          modelFilter, filterData);

      // Insert filterId, filterParentId
      int filterParentId = getIntFromCursor(cursorRuleFilters, 
          RuleFilterDbAdapter.KEY_PARENTRULEFILTERID);
      parentIds.put(filter.getDatabaseId(), filterParentId);
      
      // Store the filter instance itself.
      filtersUnlinked.put(filter.getDatabaseId(), filter);
    }
    cursorRuleFilters.close();

    // Keep iterating over all filters until we link each instance. This can be
    // improved if we know we'll get the filter records ordered in a tree hierarchy,
    // otherwise we can stick with this - we just keep passing over all the records
    // until we reconstruct each parent-child relationship.
    HashMap<Integer, RuleNode> filtersLinked = new HashMap<Integer, RuleNode>();
    Iterator<Integer> it = filtersUnlinked.keySet().iterator();
    while (it.hasNext()) {
      Integer filterId = it.next();
      ModelRuleFilter filter = filtersUnlinked.get(filterId);
      Integer parentFilterId = parentIds.get(filterId);

      if (parentFilterId.intValue() < 1) {
        // This is a top-level filter, its parent is the root node.
        RuleNode node = rootEvent.addChild(filter);
        filtersLinked.put(filterId, node);
        it.remove();
      } else {
        // Add this filter to its parent node. The node may not have
        // been constructed yet, so we have to skip it and handle it
        // on a subsequent iteration.
        RuleNode nodeParent = filtersLinked.get(parentFilterId);
        if (nodeParent != null) {
          RuleNode nodeChild = nodeParent.addChild(filter);
          filtersLinked.put(filterId, nodeChild);
          it.remove();
        }
      }
    }
  }

  /**
   * Get all actions associated with a saved rule
   */
  private ArrayList<ModelRuleAction> getActionsForRule(int ruleId) {
    Cursor cursorRuleActions = ruleActionDbAdapter.fetchAll(Long.valueOf(ruleId), null);
    
    ArrayList<ModelRuleAction> ruleActionList = new ArrayList<ModelRuleAction>(
        cursorRuleActions.getCount());
    
    while (cursorRuleActions.moveToNext()) {
      
      long ruleActionID = getLongFromCursor(cursorRuleActions, 
          RuleActionDbAdapter.KEY_RULEACTIONID);
      
      // Get the action
      ModelAction action = actions.get(
          getIntFromCursor(cursorRuleActions, RuleActionDbAdapter.KEY_ACTIONID));
      
      // Get parameters of this action
      ArrayList<ModelParameter> actionParameters = action.getParameters();
      
      // Get user data
      Cursor cursorRuleActionParameters = ruleActionParameterDbAdapter.fetchAll(
          ruleActionID, null, null);
      
      int count = cursorRuleActionParameters.getCount();
      
      ArrayList<DataType> userData = new ArrayList<DataType>(count);      
      
      for (int j = 0; j < count; j++) {
        cursorRuleActionParameters.moveToNext();
        
        DataType data = getDataType(actionParameters.get(j).getDatatype(), 
            getStringFromCursor(cursorRuleActionParameters, 
                RuleActionParameterDbAdapter.KEY_RULEACTIONPARAMETERDATA));
        
        userData.add(data);
      }
      
      ModelRuleAction ruleAction = new ModelRuleAction(
          getIntFromCursor(cursorRuleActions, RuleActionDbAdapter.KEY_RULEACTIONID),
          action, userData);
      
      cursorRuleActionParameters.close();
      
      ruleActionList.add(ruleAction);
    }
    cursorRuleActions.close();
    return ruleActionList;
  }

  /**
   * Given a rule, save it to the database.
   * 
   * @return id of the saved rule record
   * 
   * @throws IllegalStateException
   *           if this helper is closed
   */
  public long saveRule(Rule rule) throws Exception {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    
    ModelEvent event = (ModelEvent) rule.getRootNode().getItem();
    ArrayList<RuleNode> ruleFilterList = rule.getFilterBranches();
    ArrayList<ModelRuleAction> ruleActionList = rule.getActions();

    Long ruleID = Long.valueOf(rule.getDatabaseId());
    
    if (ruleID > 0) {
      deleteRule(ruleID.intValue());
    }
    
    // TODO(ehotou) need to specify rule name and desc after UI implement them
    ruleID = ruleDbAdapter.insert(Long.valueOf(event.getDatabaseId()), "New Rule", "", true);

    // Create all ruleAction records
    for (ModelRuleAction ruleAction : ruleActionList) {
      
      long ruleActionID = ruleActionDbAdapter.insert(ruleID, Long.valueOf(
          ruleAction.getModelAction().getDatabaseId()));  
      
      ArrayList<ModelParameter> parameterList = ruleAction.getModelAction().getParameters();
      ArrayList<DataType> dataList = ruleAction.getDatas();
      for (int i = 0; i < dataList.size(); i++) {
        ruleActionParameterDbAdapter.insert(ruleActionID, 
            Long.valueOf(parameterList.get(i).getDatabaseId()), dataList.get(i).toString());
      }
    }

    // Save all rule filters
    for (RuleNode filterNode : ruleFilterList) {
      saveFilterRuleNode(ruleID, -1, filterNode);
    }
    
    return ruleID;
  }

  /**
   * Recursively write each node of the filter branches to the database.
   * 
   * @param node
   *          is root of the ruleFilterNode tree to be added
   */
  private void saveFilterRuleNode(long ruleID, long parentRuleNodeID, RuleNode node) {

    ModelRuleFilter filter = (ModelRuleFilter) node.getItem();
    
    long ruleFilterID = ruleFilterDbAdapter.insert(ruleID, 
        Long.valueOf(filter.getModelFilter().getAttribute().getDatabaseId()), 
        Long.valueOf(-1), // TODO(ehotou) after implementing external, insert it here
        // TODO: (ehotou) verify ModelFilter id is what we want here (not ModelRuleFilter):
        Long.valueOf(filter.getModelFilter().getDatabaseId()), 
        parentRuleNodeID, 
        filter.getData().toString());

    // insert all children filters recursively:
    for (RuleNode filterNode : node.getChildren()) {
      saveFilterRuleNode(ruleID, ruleFilterID, filterNode);
    }
  }
  
  /**
   * Delete a rule record as well as ruleAction, ruleActionParameter and ruleFilters records
   * associated with it.
   * 
   * @param ruleID
   *          is id of the rule to be delete
   */
  public void deleteRule(int ruleID) {
    if (isClosed) {
      throw new IllegalStateException("UIDbHelper is closed.");
    }
    
    Rule rule = loadRule(ruleID);
    ruleDbAdapter.delete(Long.valueOf(ruleID));
    
    ArrayList<ModelRuleAction> ruleActionList = rule.getActions();
    for (ModelRuleAction ruleAction : ruleActionList) {
      ruleActionDbAdapter.delete(Long.valueOf(ruleAction.getDatabaseId()));
      
      // Delete all rule parameters
      Cursor cursorRuleParameters = ruleActionParameterDbAdapter.fetchAll(
          Long.valueOf(ruleAction.getDatabaseId()), null, null);
      while(cursorRuleParameters.moveToNext()) {
        ruleActionParameterDbAdapter.delete(getLongFromCursor(cursorRuleParameters, 
            RuleActionParameterDbAdapter.KEY_RULEACTIONPARAMETERID));
      }
      cursorRuleParameters.close();
    }
    
    ArrayList<RuleNode> ruleFilterList = rule.getFilterBranches();
    for (RuleNode filterNode : ruleFilterList) {
      deleleFilterRuleNode(filterNode);
    }
  }
  
  /**
   * Recursively delete each node of the filter branches from the database.
   * 
   * @param node
   *          is root of the ruleFilterNode tree to be deleted
   */
  private void deleleFilterRuleNode(RuleNode node) {
    ModelRuleFilter ruleFilter = (ModelRuleFilter) node.getItem();
    ruleFilterDbAdapter.delete(Long.valueOf(ruleFilter.getDatabaseId()));
    for (RuleNode childRuleFilterNode : node.getChildren()) {
      deleleFilterRuleNode(childRuleFilterNode);
    }
  }
}
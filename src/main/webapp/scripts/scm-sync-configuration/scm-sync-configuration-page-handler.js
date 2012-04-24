YAHOO.namespace("scmSyncConfiguration");
YAHOO.scmSyncConfiguration.targetFormName = '${pageMatcher.targetFormName}';
YAHOO.scmSyncConfiguration.commentPopupValidated = false;

function retrieveTargetFormOnPage(){
    var formElts = $$("form");
    for(var i=0; i<formElts.length; i++){
        var form = formElts[i];
        // Tested under IE8 & Chrome : it works well
        // On stackoverflow, doubt are there about IE7 for this implementation ...
        if(form.attributes["name"] != null && form.attributes["name"].value == YAHOO.scmSyncConfiguration.targetFormName){
            return form;
        }
        // Don't work under IE8.. will work with IE7 ?
        if(form.getAttribute("name") == YAHOO.scmSyncConfiguration.targetFormName){
            return form;
        }
    }
    return null;
}
function decorateOnsubmitForm(){
    try {
        var form = retrieveTargetFormOnPage();
        if(form != null){
            form.observe("submit", function(evt){
                var dontStopEvent = YAHOO.scmSyncConfiguration.commentPopupValidated || enterCommitComment();
                if(!dontStopEvent){
                    Event.stop(evt);
                }
            });
        } else {
            unexpectedError("Cannot retrieve target form on current page !");
        }
    }catch(ex){ unexpectedError("Exception: "+logProps(ex)); }
    form = null; // memory leak prevention
}

function unexpectedError(message){
    alertMsg = 'Something went wrong with the scm-sync-configuration plugin !\n';
    alertMsg += 'Please report a JIRA with as much informations as possible (browser, os, current url, error message etc.).\n';
    alertMsg += 'Error message : '+message;
    alert(alertMsg);
}

/**
  * Log l'ensemble des propriétés de l'objet javascript fourni
  */
function logProps(obj){
    str = '';
    if(obj == null){
        return "unknown object (null) !";
    } else {
        for(prop in obj)
        {
            str += '['+prop+'='+obj[prop]+'],';
        }
        return "Object : "+obj+" :: "+str;
    }
}

function ajaxCall(callType, param, successCallback){
    ajaxCall(callType, param, successCallback, false);
}

function ajaxCall(callType, param, successCallback, skipLoading){

    if(!skipLoading){
        YAHOO.namespace("scm.sync.configuration.wait");
        YAHOO.scm.sync.configuration.wait.modalPopup =
            new YAHOO.widget.Panel("wait",
                { width:"240px",
                  fixedcenter:true,
                  close:false,
                  draggable:false,
                  zindex:4,
                  modal:true
                }
            );

        YAHOO.scm.sync.configuration.wait.modalPopup.setHeader("--- Waiting ---");
        YAHOO.scm.sync.configuration.wait.modalPopup.setBody("--- Waiting ---");
        YAHOO.scm.sync.configuration.wait.modalPopup.render(document.body);
    }

    var ajaxCallParams = {
        onSuccess: function(ret) {
            successCallback.call(null, ret);
            if(!skipLoading){
                YAHOO.scm.sync.configuration.wait.modalPopup.hide();
            }
        },/* For unknown reasons, an exception is thrown after the onSuccess process .. :(
        onException: function(transport, ex) {
            alert('exception : '+ex);
            if(!skipLoading){
                YAHOO.scm.sync.configuration.wait.modalPopup.hide();
            }
            throw ex;
        },*/
        onFailure: function(transport) {
            alert('failure : '+Object.toJSON(transport));
            if(!skipLoading){
                YAHOO.scm.sync.configuration.wait.modalPopup.hide();
            }
        }
    };

    if(callType == 'form'){
        $(param).request(ajaxCallParams);
    } else {
        new Ajax.Request(param, ajaxCallParams);
    }
}

function enterCommitComment(form){
    YAHOO.namespace("scm.sync.configuration");
    YAHOO.scm.sync.configuration._buttons = [];
    YAHOO.scm.sync.configuration.handleSubmit = function() {
        ajaxCall('form', 'commentForm', function(ret){
            YAHOO.scmSyncConfiguration.commentPopupValidated = true;
            YAHOO.scm.sync.configuration.modalPopup.hide();
            retrieveTargetFormOnPage().submit();
        }, true);
    }
    YAHOO.scm.sync.configuration.handleCancel = function() {
        YAHOO.scm.sync.configuration.modalPopup.hide();
    }

    YAHOO.scm.sync.configuration.modalPopup =
        new YAHOO.widget.Panel("buildStatConfigForm",
            { width:"720px",
              fixedcenter:true,
              close:false,
              draggable:false,
              zindex:4,
              modal:true
            }
        );

    var currentContext = createTemplateContext();
    var popupContentTemplate = new Template(getTemplateContent('popupContentTemplate'));
    content = popupContentTemplate.evaluate(currentContext);

    YAHOO.scm.sync.configuration.modalPopup.setHeader("Commit comment");
    YAHOO.scm.sync.configuration.modalPopup.setBody(content);
    YAHOO.scm.sync.configuration.modalPopup.setFooter('<span id="panelFooter" class="button-group"></span>');
    YAHOO.scm.sync.configuration.modalPopup.showEvent.subscribe(function() {
        if (this._buttons.length == 0) {
            this._buttons[0] = new YAHOO.widget.Button({
                type: 'button',
                label: "Submit comment",
                container: 'panelFooter'
            });
            this._buttons[0].on('click', YAHOO.scm.sync.configuration.handleSubmit);
            this._buttons[1] = new YAHOO.widget.Button({
                type: 'button',
                label: "Cancel",
                container: 'panelFooter'
            });
            this._buttons[1].on('click', YAHOO.scm.sync.configuration.handleCancel);
        }
    }, YAHOO.scm.sync.configuration, true);
    YAHOO.scm.sync.configuration.modalPopup.render(document.body);

    return false;
}

// For some unknown reasons, on firefox, some #{XXX} template variables are replaced by #%7BXXX%7D :(
function getTemplateContent(templateId){
    var content = $(templateId).innerHTML;
    content = content.replace(new RegExp("%7B", "g"), "{");
    content = content.replace(new RegExp("%7D", "g"), "}");
    return content;
}

function createTemplateContext(){
    // Creating context for creation
    var currentContext = {
        rootURL: rootURL
    };
    return currentContext;
}

import logging
from authlib.integrations.flask_client.apps import FlaskOAuth2App
from superset.security import SupersetSecurityManager

class CustomSsoSecurityManager(SupersetSecurityManager):

    def oauth_user_info(self, provider, response=None):
        logging.debug("Oauth2 provider: {0}.".format(provider))
        if provider == 'egaSSO':
            # As example, this line request a GET to base_url + '/' + userDetails with Bearer  Authentication,
    # and expects that authorization server checks the token, and response with user details
            dictApp = self.appbuilder.sm.oauth_remotes[provider]._get_requested_token()
            logging.debug("************dictApp: {0}".format(dictApp))
            # oauth_remote[provider]:authlib.integrations.flask_client.OAuth.register：LocalProxy：FlaskOAuth2App
            respond = self.appbuilder.sm.oauth_remotes[provider].get('userinfo')
            # flaskApp = FlaskOAuth2App
            # flaskApp.fetch_access_token
            jsonData = respond.json()
            #logging.debug("user_data: {0}".format(jsonData))
            #logging.debug("*****************: {0}".format(respond.content))
            #logging.debug("*****************: {0}".format(dir(respond)))
            m2 = jsonData['data']
            logging.debug("m2_responn: {0}".format(m2))
            # return { 'name' : m2['nickname'], 'email' : '233@123.com', 'id' : m2['age'], 'username' : m2['nickname'], 'first_name':'kk', 'last_name':'jz'}
            return { 'name' : m2['nickname'], 'email' : m2['email'], 'username' : m2['username'], 'first_name': m2['first_name'], 'last_name': m2['last_name']}



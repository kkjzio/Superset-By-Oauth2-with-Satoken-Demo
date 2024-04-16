from flask_appbuilder.security.manager import AUTH_OAUTH
from custom_sso_security_manager import CustomSsoSecurityManager
CUSTOM_SECURITY_MANAGER = CustomSsoSecurityManager


# 启动数据源
SQLALCHEMY_DATABASE_URI = "mysql://root:123456@192.168.59.129:3306/superset?charset=utf8"
# 启动语言设置中文
BABEL_DEFAULT_LOCALE = "zh"

# 去掉X-Frame-Options限制，可以免去跨域访问问题：直接将里面置空就好了
HTTP_HEADERS = {}
# WTF_CSRF_ENABLED设置为False
WTF_CSRF_ENABLED = False
# 将PUBLIC_ROLE_LIKE 设置为Gamma
# PUBLIC_ROLE_LIKE = "Gamma"

# setRole
# FAB_ROLES = {
#     "ReadOnly": [
#         ["can_list_on_Dashboard", "my_can_list"],
#         ["can_read_on_Dashboard", "my_can_read"]
#     ]
# }
# FAB_ROLES = {
#     'ReadOnly': {
#         'can_list': ['TableModelView', 'SqlMetricsListView'],
#         'can_show': ['TableModelView', 'SqlMetricsListView'],
#         'can_get': ['TableModelView', 'SqlMetricsListView'],
#     }
# }

#enable swaage api
FAB_API_SWAGGER_UI = True

#enable filters
FEATURE_FLAGS = {"DASHBOARD_FILTERS_EXPERIMENTAL": True, "DASHBOARD_NATIVE_FILTERS_SET": True, "DASHBOARD_NATIVE_FILTERS": True, "DASHBOARD_CROSS_FILTERS": True, "ENABLE_TEMPLATE_PROCESSING": True}


# Set the authentication type to OAuth
AUTH_TYPE = AUTH_OAUTH

OAUTH_PROVIDERS = [
    {   'name':'egaSSO',
        'token_key':'access_token', # Name of the token in the response of access_token_url
        'icon':'fa-address-card',   # Icon for the provider
        'remote_app': {
            'client_id':'1001',  # Client Id (Identify Superset application)
            'client_secret':'aaaa-bbbb-cccc-dddd-eeee', # Secret for this Client Id (Identify Superset application)
            'access_token_method':'GET',
            'client_kwargs':{
                'scope': 'userinfo',
            },
            'access_token_params':{        # Additional parameters for calls to access_token_url
                'client_id':'1001',
                'client_secret':'aaaa-bbbb-cccc-dddd-eeee'
            },
            'access_token_method':'POST',    # HTTP Method to call access_token_url
            'api_base_url':'http://192.168.59.1:8001/oauth2/',
            'access_token_url':'http://192.168.59.1:8001/oauth2/token',
            'authorize_url':'http://192.168.59.1:8001/oauth2/authorize'
        }
    }
]


# Test
#for provider in OAUTH_PROVIDERS:
#    print(provider['remote_app']['client_kwargs'])

# Will allow user self registration, allowing to create Flask users from Authorized User
AUTH_USER_REGISTRATION = True

# The default user self registration role
#AUTH_USER_REGISTRATION_ROLE = "Public"
#AUTH_USER_REGISTRATION_ROLE = "ReadOnly"




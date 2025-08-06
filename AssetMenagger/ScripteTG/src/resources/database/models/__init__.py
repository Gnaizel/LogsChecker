from .messages import MessageModel
from .settings import SettingsModel
from .users import UserModel
from .whitelist import WhiteListModel
from .. import database


def register_models() -> None:
    """
    Register all database models
    """

    database.create_tables([MessageModel, WhiteListModel, UserModel, SettingsModel])

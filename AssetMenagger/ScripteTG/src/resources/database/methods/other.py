from database.methods.create import create_user, create_settings
from database.methods.get import get_user, get_settings
from database.methods.update import update_user
from database.models import SettingsModel
from database.models.users import UserModel


def get_or_create_user(user_id: int, first_name: str, last_name: str, username: str = None) -> UserModel:
    user = get_user(user_id)

    if user:
        user = update_user(user, first_name, last_name, username)

        return user

    return create_user(user_id, first_name, last_name, username)


def get_or_create_settings(user_id: int) -> SettingsModel:
    settings = get_settings(user_id)

    if not settings:
        return create_settings(user_id)

    return settings

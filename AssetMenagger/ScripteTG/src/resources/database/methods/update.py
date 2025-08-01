from database.models import MessageModel, SettingsModel
from database.models.users import UserModel


def update_message_deleted(message_model: MessageModel, deleted: bool) -> MessageModel:
    message_model.deleted = deleted

    message_model.save()

    return message_model


def update_message_edited(message_model: MessageModel, edited: bool) -> MessageModel:
    message_model.edited = edited

    message_model.save()

    return message_model


def update_message_text(message_model: MessageModel, text: str) -> MessageModel:
    message_model.text = text

    message_model.save()

    return message_model


def update_user(user: UserModel, first_name: str, last_name: str, username: str = None) -> UserModel:
    user.first_name = first_name
    user.last_name = last_name
    user.username = username
    user.save()

    return user


def update_user_settings(settings: SettingsModel, *,
                         save_pm: bool = True,
                         save_chat: bool = True,
                         save_channel: bool = True) -> SettingsModel:
    settings.save_pm = save_pm
    settings.save_chat = save_chat
    settings.save_channel = save_channel
    settings.save()

    return settings

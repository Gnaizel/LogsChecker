import time
from typing import Union

from database.models import MessageModel, WhiteListModel, SettingsModel
from database.models.users import UserModel


def create_message(*, text: Union[str, None], message_id: int, from_user_id: int,
                   media: Union[str, None], voice_note: bool, sticker: bool,
                   saved_message_id: Union[int, str]) -> MessageModel:
    model = MessageModel.create(
        text=text,
        message_id=message_id,
        from_user_id=from_user_id,
        media=media,
        voice_note=voice_note,
        sticker=sticker,
        saved_message_id=saved_message_id,
        created=time.time()
    )

    model.save()

    return model


def add_whitelist(chat_id: int) -> WhiteListModel:
    model = WhiteListModel.create(
        chat_id=chat_id
    )

    model.save()

    return model


def create_user(user_id: int, first_name: str, last_name: str, username: str = None) -> UserModel:
    user = UserModel.create(user_id=user_id, first_name=first_name, last_name=last_name, username=username)

    return user


def create_settings(user_id: int) -> SettingsModel:
    settings = SettingsModel.create(user_id=user_id)

    return settings

from typing import Union, List, Optional

from database.models.users import UserModel

from database.models import WhiteListModel, SettingsModel
from database.models.messages import MessageModel


def get_all_messages() -> Union[List[MessageModel], None]:
    query = MessageModel.select()

    messages = [message for message in query]

    return messages


def get_message_by_id(message_id) -> Optional[MessageModel]:
    return MessageModel.get_or_none(MessageModel.message_id == message_id)


def get_all_whitelist_ids() -> Union[List[int], None]:
    query = WhiteListModel.select()

    ids = [whitelist.chat_id for whitelist in query]

    return ids


def get_user(user_id: int) -> Optional[UserModel]:
    return UserModel.get_or_none(UserModel.user_id == user_id)


def get_settings(user_id: int) -> Optional[SettingsModel]:
    return SettingsModel.get_or_none(SettingsModel.user_id == user_id)

from database.models import WhiteListModel, MessageModel

from .. import database


def delete_whitelist_id(chat_id) -> None:
    row = WhiteListModel.get(WhiteListModel.chat_id == chat_id)
    row.delete_instance()


def clear_messages() -> None:
    with database.atomic():
        for row in MessageModel.select():
            row.delete_instance()


def clear_whitelist() -> None:
    with database.atomic():
        for row in WhiteListModel.select():
            row.delete_instance()

from peewee import IntegerField

from .. import BaseModel


class WhiteListModel(BaseModel):
    chat_id = IntegerField()

    class Meta:
        table_name = "whitelist"

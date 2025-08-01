from peewee import BooleanField, IntegerField

from .. import BaseModel


class SettingsModel(BaseModel):

    user_id = IntegerField()
    save_pm = BooleanField(default=True)
    save_chat = BooleanField(default=True)
    save_channel = BooleanField(default=True)

    class Meta:
        table_name = "settings"

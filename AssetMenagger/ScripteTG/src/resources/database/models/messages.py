from peewee import IntegerField, TextField, DateField, BooleanField

from .. import BaseModel


class MessageModel(BaseModel):

    message_id = IntegerField()
    from_user_id = IntegerField()
    text = TextField(null=True)
    media = TextField(null=True)
    saved_message_id = IntegerField(null=True)
    voice_note = BooleanField(default=False)
    sticker = BooleanField(default=False)
    created = DateField()
    deleted = BooleanField(default=False)
    edited = BooleanField(default=False)

    class Meta:
        table_name = "messages"

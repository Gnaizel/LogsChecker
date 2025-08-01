from peewee import IntegerField, TextField

from .. import BaseModel


class UserModel(BaseModel):
    user_id = IntegerField()
    first_name = TextField()
    last_name = TextField(null=True)
    username = TextField(null=True)

    class Meta:
        table_name = "users"

import peewee
from peewee import Model

import utils

PATHS: dict = utils.get_paths()

database = peewee.SqliteDatabase(PATHS.get("DATABASE_PATH"))


class BaseModel(Model):
    class Meta:
        database = database

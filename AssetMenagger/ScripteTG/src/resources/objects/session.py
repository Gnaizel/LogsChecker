import asyncio
import datetime
import json
import os
import string
from dataclasses import dataclass
from typing import Union, Type, Dict, List, Optional

import telethon
from peewee import fn
from telethon import TelegramClient, events
from telethon.errors import UsernameOccupiedError, \
    UsernameNotModifiedError, UnauthorizedError
from telethon.tl.types import User, Message, Channel, Chat, MessageMediaPhoto, \
    MessageMediaDocument, DocumentAttributeAudio, DocumentAttributeSticker, DocumentAttributeVideo, \
    DocumentAttributeAnimated

import utils
from data import config
from database.methods.create import create_message, add_whitelist
from database.methods.delete import delete_whitelist_id, clear_messages
from database.methods.get import get_all_messages, get_all_whitelist_ids, get_message_by_id, \
    get_user
from database.methods.other import get_or_create_user, get_or_create_settings
from database.methods.update import update_message_deleted, update_message_text, update_user_settings, \
    update_message_edited
from database.models import MessageModel, UserModel
from objects.logger import LoggerObject

PATHS: Dict[str, str] = utils.get_paths()


@dataclass(order=True)
class UserData:
    """
    Dataclass for storing user data
    :var session_file: str - name of file where user data is stored
    :var phone: str - phone number of the user
    :var app_id: str - app id of the user
    :var app_hash: str - app hash of the user
    :var first_name: str - first name of the user
    :var last_name: str - last name of the user
    :var username: Union[str, None] - username of the user
    :var twoFA: Union[str, None] - two-factor auth code for the user
    :var user_id: int - user id of the user
    :var device: str - the device from which the user login in
    :var app_version: str - app version of the user
    :var lang_pack: str - language of the user
    :var system_lang_pack: str - system language of the user
    :var proxy: tuple - tuple with proxy. (ProxyType, host, port, rdns, username, password)
    """
    session_file: str
    phone: str
    app_id: int
    app_hash: str
    first_name: str
    last_name: str
    username: Union[str, None]
    twoFA: Union[str, None]
    user_id: int
    device: str
    app_version: str
    lang_pack: str
    system_lang_pack: str
    proxy: tuple


class Session:

    def __init__(self, *, app_id: int, app_hash: str, path_to_session: str, device: str = "Xiaomi Redmi Note 7",
                 app_version: str = "0.21.8.1166-armeabi-v7a", lang_pack: str = "android",
                 system_lang_pack: str = "en-Us", two_fa: str = None, proxy: Union[tuple, None] = None,
                 log: bool = True,
                 logger: LoggerObject = None):
        """
        Session base class
        :param app_id: int - app id of the user
        :param app_hash: str - app hash of the user
        :param path_to_session: str - path to the session file
        :param device: str - device of the user
        :param app_version: str - app version of the user
        :param lang_pack: str - language of the user
        :param system_lang_pack: str - system language
        :param proxy: tuple - tuple with proxy
        :param log: bool - logging information?
        :param logger: LoggerObject - logger object
        """
        self.__app_id = app_id
        self.__app_hash = app_hash
        self.__path_to_session = path_to_session
        self.__path_to_json = self.__path_to_session.replace(".session", ".json")
        self.__session_file = path_to_session.split(os.sep)[-1].replace(".session", "")
        self.__json_file = f"{self.__session_file}.json"
        self.__device = device if device is not None else "Xiaomi Redmi Note 7"
        self.__app_version = app_version if app_version is not None else "0.21.8.1166-armeabi-v7a"
        self.__lang_pack = lang_pack if lang_pack is not None else "android"
        self.__system_lang_pack = system_lang_pack if system_lang_pack is not None else "en-Us"
        self.__proxy = proxy
        self.__two_fa = two_fa

        self.log = log
        self.logger = logger

        # бот
        self.bot = TelegramClient('bot', self.__app_id, self.__app_hash).start(bot_token=config.BOT_TOKEN)

        # self.user_data = UserData()

        # Если сессия с прокси

        self.client: TelegramClient = TelegramClient(
            self.__path_to_session,
            self.__app_id,
            self.__app_hash,
            system_version="4.16.30-vxCUSTOM",
            device_model=self.__device,
            app_version=self.__app_version,
            lang_code=self.__lang_pack,
            system_lang_code=self.__system_lang_pack
        )

        # Если сессия без прокси
        if not self.__proxy and self.log:
            self.logger.log(f"Сессия {self.__session_file} без прокси.")
        else:
            self.logger.log(f"Сессия {self.__session_file} с прокси.")

            # устаналиваем прокси
            self.client.set_proxy(self.__proxy)

    async def _reconnect_proxy(self):
        """
        Reconnect until the proxy works
        """
        while True:

            if self.log:
                self.logger.warning(
                    f"Сессия {self.__session_file}. "
                    f"Ошибка с подключением к прокси [{self.__proxy[1]}:{self.__proxy[2]}]")

            try:
                await self.client.connect()
            except OSError:
                # ошибка все ещё осталась
                continue

            if self.client.is_connected():
                break

            await asyncio.sleep(1)

    async def set_username(self) -> str:
        """
        Set username for session
        :return: username
        """
        try:
            async with self.client as cl:
                while True:
                    try:
                        username = utils.id_generator(chars=string.ascii_lowercase)
                        await cl(
                            telethon.functions.account.UpdateUsernameRequest(
                                username=username
                            )
                        )
                        return username

                    except (UsernameOccupiedError, UsernameNotModifiedError):
                        if self.log:
                            self.logger.warning(f"Сгенерирован одинаковый юзернейм. Перегенерируем.")

                        continue

        except OSError:
            await self._reconnect_proxy()
            return await self.set_username()

    async def valid(self, *, set_username: bool = False) -> Union[bool, UserData]:
        """
        Validate the session
        :param set_username: bool - set username if it is not exists
        :return: True if the session is valid, False if the session is not valid
        """
        # если клиент не подключен, подключаемся
        if not self.client.is_connected():
            try:
                await self.client.connect()
            except OSError:
                await self._reconnect_proxy()

        # авторизовываемся
        try:
            if self.__two_fa is not None:
                await self.client.sign_in(password=self.__two_fa)
            else:
                await self.client.sign_in()
        except UnauthorizedError:
            if self.log:
                self.logger.warning(f"Сессия {self.__session_file} невалидна.")

            # перемещаем в папку с забаненными сессиями
            utils.move_file(from_path=self.__path_to_session,
                            to_path=f"{PATHS.get('SESSIONS_NOT_WORK_PATH')}{os.sep}{self.__session_file}")
            utils.move_file(from_path=self.__path_to_json,
                            to_path=f"{PATHS.get('SESSIONS_NOT_WORK_PATH')}{os.sep}{self.__json_file}")

            return False
        except OSError:
            await self._reconnect_proxy()
            return await self.valid()

        # если клиент не авторизован, авторизовываемся
        if not await self.client.is_user_authorized():
            return False

        # получаем данные о клиенте
        me: User = await self.client.get_me()

        if set_username and not me.username:
            name = await self.set_username()

            if self.log:
                self.logger.log(f"Установили юзернейм {name} для сессии {self.__session_file}")

        user_data: UserData = UserData(
            session_file=self.__session_file,
            phone=me.phone,
            app_id=self.__app_id,
            app_hash=self.__app_hash,
            first_name=me.first_name,
            last_name=me.last_name,
            username=me.username,
            twoFA=self.__two_fa,
            user_id=me.id,
            device=self.__device,
            app_version=self.__app_version,
            lang_pack=self.__lang_pack,
            system_lang_pack=self.__system_lang_pack,
            proxy=self.__proxy
        )

        # обновляем json сессии

        with open(self.__path_to_json, "r", encoding="utf-8") as f:
            data: dict = json.load(f)

        data.update(user_data.__dict__)

        with open(self.__path_to_json, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=4)

        return user_data


class WorkerSession(Session):
    def __init__(self, *, app_id: int, app_hash: str, path_to_session: str, device: str = "Xiaomi Redmi Note 7",
                 app_version: str = "0.21.8.1166-armeabi-v7a", lang_pack: str = "android",
                 system_lang_pack: str = "en-Us", two_fa: str = None, proxy: Union[tuple, None] = None,
                 log: bool = True, logger: LoggerObject = None):
        super().__init__(app_id=app_id, app_hash=app_hash, path_to_session=path_to_session, device=device,
                         app_version=app_version, lang_pack=lang_pack, system_lang_pack=system_lang_pack, two_fa=two_fa,
                         proxy=proxy, log=log, logger=logger)
        self.user_data: Type[UserData] = UserData

    async def valid(self, *, set_username: bool = False) -> bool:
        valid = await super().valid(set_username=set_username)

        if not valid:
            return False

        self.user_data = valid

        return True

    # хандлеры

    async def handle_edit_message(self, event):
        event: events.MessageEdited.Event

        # получаем старое сообщение из бд
        old_message: Optional[MessageModel] = get_message_by_id(event.message.id)

        if old_message:
            if not old_message.text.strip() == event.message.message.strip():

                # отправляем сообщение пидорасу, ой, пользователю

                sender = await event.get_sender()

                if sender.bot:
                    return

                text = (f"<b>✏️ {sender.first_name} изменил текст сообщения.</b>\n"
                        f"<b>Старый текст:</b> {old_message.text}\n"
                        f"<b>Новый текст:</b> {event.message.message}")

                # обновляем текст в бд
                update_message_text(old_message, event.message)
                update_message_edited(old_message, True)

                await self.bot.send_message(entity=self.user_data.user_id, message=text, parse_mode="HTML")

    async def handle_delete_message(self, event):
        event: events.MessageDeleted.Event

        deleted_messages: List[MessageModel] = [message for message in get_all_messages() if
                                                message.message_id in event.deleted_ids]

        for message in deleted_messages:

            # отправляем сообщение об удалении
            entity = await self.client.get_entity(message.from_user_id)

            text = ""
            update_message_deleted(message_model=message, deleted=True)

            user_settings = get_or_create_settings(self.user_data.user_id)

            if entity.id in get_all_whitelist_ids():
                return

            if isinstance(entity, User):

                if entity.id == self.user_data.user_id or entity.bot:
                    return

                if not user_settings.save_pm:
                    return

                if event.chat_id:

                    entity2 = await self.client.get_entity(event.chat_id)

                    text = (
                        f"<b>🗑 {(entity.first_name + ' ' + entity.last_name) if entity.last_name else entity.first_name} "
                        f"удалил сообщение в чате {entity2.title}. Содержимое: </b>")

                else:

                    text = (
                        f"<b>🗑 {(entity.first_name + ' ' + entity.last_name) if entity.last_name else entity.first_name} "
                        f"удалил сообщение в личке. Содержимое: </b>")

            elif isinstance(entity, Channel):

                if not user_settings.save_channel:
                    return

                # канал

                if not entity.megagroup:
                    text = f"<b>🗑 Удалено сообщение в канале {entity.title}. Содержимое: </b>"
                else:

                    db_user: UserModel = get_user(message.from_user_id)

                    if db_user:
                        text = f"<b>🗑 Удалено сообщение в чате {entity.title} от {(db_user.first_name + ' ' + db_user.last_name) if db_user.last_name else db_user.first_name}. Содержимое: </b>"
                    else:
                        text = f"<b>🗑 Удалено сообщение в чате {entity.title}. Содержимое: </b>"

            elif isinstance(entity, Chat):

                if not user_settings.save_chat:
                    return

                text = f"<b>🗑 Удалено сообщение в группе {entity.title}. Содержимое: </b>"
            else:
                return

            if message.text:
                text += f"{message.text}"

            text = text.strip()

            if message.sticker:
                await self.bot.send_message(entity=self.user_data.user_id, message=text,
                                            parse_mode="HTML")
                # отправка стикера

                await self.bot.send_file(entity=self.user_data.user_id,
                                         file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{message.media}")

                return

            if message.media:

                if text:
                    await self.bot.send_file(entity=self.user_data.user_id, caption=text,
                                             file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{message.media}",
                                             parse_mode="HTML", voice_note=message.voice_note)
                else:
                    await self.bot.send_file(entity=self.user_data.user_id,
                                             file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{message.media}",
                                             parse_mode="HTML", voice_note=message.voice_note)

            else:
                await self.bot.send_message(entity=self.user_data.user_id, message=text, parse_mode="HTML")

    async def new_message(self, event):
        event: events.NewMessage.Event

        message: Message = event.message

        text = None
        media = None

        sender = await event.get_sender()

        if not sender:
            return

        if sender.id == self.user_data.user_id:
            # хандлим команды

            if not message.message.startswith(config.COMMAND_PREFIX):
                return

            split = message.message.split(" ")
            cmd = split[0].replace(config.COMMAND_PREFIX, "").lower().strip()

            split.pop(0)

            args = split

            if cmd == "whitelist":
                whitelist_ids = get_all_whitelist_ids()

                user_id = event.chat_id

                reply = await event.get_reply_message()

                if reply and isinstance(reply.sender, User):
                    user_id = reply.sender.id

                if user_id in whitelist_ids:

                    delete_whitelist_id(chat_id=user_id)

                    await event.edit("<b>Пользователь удалён из белого списка!</b>", parse_mode="HTML")

                else:

                    add_whitelist(chat_id=user_id)

                    await event.edit("<b>Пользователь добавлен в белый список!</b>", parse_mode="HTML")

            elif cmd == "chatmode":
                user_settings = get_or_create_settings(self.user_data.user_id)

                if not user_settings.save_chat and not user_settings.save_channel:

                    update_user_settings(user_settings, save_chat=True, save_channel=True)

                    await event.edit("<b>Сохранение с чатов и каналов включено</b>", parse_mode="HTML")
                else:

                    update_user_settings(user_settings, save_chat=False, save_channel=False)

                    await event.edit("<b>Сохранение с чатов и каналов выключено</b>", parse_mode="HTML")

            elif cmd == "stat":
                # Получаем минимальное время создания сообщения из базы данных
                oldest_message_time = MessageModel.select(fn.Min(MessageModel.created)).scalar()

                if oldest_message_time:
                    # Преобразуем timestamp в объект datetime
                    oldest_message_datetime = datetime.datetime.fromtimestamp(oldest_message_time)

                    # Форматируем объект datetime в строку в нужном формате
                    oldest_message = oldest_message_datetime.strftime('%Y-%m-%d %H:%M:%S')
                else:
                    oldest_message = "NaN"

                text = f"📊 <b>Статистика кэша</b>\n\n📊 <b>Общий размер кэша: {utils.sizeof_fmt(os.path.getsize(PATHS.get('DATABASE_PATH')))}</b>\n📥 <b>Сохраненные сообщения: {len(get_all_messages())} шт.</b>\n🕒 <b>Самое старое сообщение: {oldest_message}</b>"

                await event.edit(text, parse_mode="HTML")

            elif cmd == "info":

                text = (
                    """<b>📩 ScripteTg Spy</b> - UserBot который сохраняет удаленные сообщения в Telegram и уведомлять вас об этом. 

<code>.info</code> - Вывести сообщение с информацией о "ScripteTg Spy" 
<code>.clear</code> - Почистить базу данных и прочие сообщения (сбросить настройки)
<code>.whitelist</code> + reply - Добавить пользователя в белый лист и не сохранять от него сообщения
<code>.stat</code> - статистика использования user бота
<code>.save</code> + reply - сохранить это сообщение
<code>.chatmode</code> - вкл/выкл сохранения сообщений с чатов и канал (осторожно, может засрать логи если много чатов на аккаунте)
<code>.rest</code> &lt;ID чата / usera&gt; - Отправить весь скачанный диалог с ним.

<b>ℹ️ Важно:</b> Автор скрипта @ScipteTg"""
                )

                await event.edit(text, parse_mode='HTML')

            elif cmd == "witeusers" or cmd == "whiteusers":

                text = f"<b>📄 Список пользователей в White List:</b>\n\n"

                for whitelist_id in get_all_whitelist_ids():

                    db_user = get_user(whitelist_id)

                    if not db_user or not db_user.username:

                        user = await self.client.get_entity(whitelist_id)

                        name = (user.first_name + ' ' + user.last_name) if user.last_name else user.first_name

                    else:
                        name = (db_user.first_name + ' ' + db_user.last_name) if db_user.last_name else db_user.first_name

                    text += f"<b><a href='tg://user?id={whitelist_id}'>• {name}</a></b>\n"

                text += ("\nДля добавления / удаления из списка \"White List\" надо написать команду .whitelist "
                         "ответом на сообщение юзера.")

                await event.edit(text, parse_mode='HTML')

            elif cmd == "save":

                if message.reply_to:

                    reply_message: Message = await self.client.get_messages(
                        event.chat_id,
                        ids=message.reply_to.reply_to_msg_id
                    )

                    text = "💾 <b>Сообщение было сохранено. Содержимое: </b>"

                    if reply_message.media:
                        await self.bot.send_file(self.user_data.user_id, file=reply_message.media, caption=text)
                    else:

                        await event.edit(f"💾 <b>Сообщение было сохранено.</b>",
                                         parse_mode="HTML")

                        await self.bot.send_message(self.user_data.user_id,
                                                    f"💾 <b>Сообщение было сохранено. Содержимое: {reply_message.message} </b>",
                                                    parse_mode="HTML")
                else:
                    await event.edit("💾 <b>Необходимо переслать сообщение, которое вы хотите сохранить!</b>",
                                     parse_mode="HTML")

            elif cmd == "clear":
                clear_messages()
                await event.edit("<b>🧹 Кэш успешно очищен</b>", parse_mode="HTML")

            elif cmd == "rest":

                if len(args) < 1:
                    await event.edit(f"<b>Вы не указали User Id</b>", parse_mode="HTML")
                    return

                user_id = args[0]

                await event.edit(f"<b>😺 Сообщения успешно восстановлены. Они будут доставлены в ЛС бота в ближайшее "
                                 f"время.</b>", parse_mode="HTML")

                messages: List[MessageModel] = get_all_messages()

                for message1 in messages:

                    if message1.from_user_id == user_id:
                        # отправляем сообщение
                        if message1.deleted:

                            # отправляем сообщение об удалении
                            entity = await self.client.get_entity(message1.from_user_id)

                            text = ""

                            user_settings = get_or_create_settings(self.user_data.user_id)

                            if entity.id in get_all_whitelist_ids():
                                return

                            if isinstance(entity, User):

                                if entity.id == self.user_data.user_id or entity.bot:
                                    return

                                if not user_settings.save_pm:
                                    return

                                text = (
                                    f"<b>🗑 {(entity.first_name + ' ' + entity.last_name) if entity.last_name else entity.first_name} "
                                    f"удалил сообщение в личке. Содержимое: </b>")

                            elif isinstance(entity, Channel):

                                if not user_settings.save_channel:
                                    return

                                # канал
                                if not entity.megagroup:
                                    text = f"<b>🗑 Удалено сообщение в канале {entity.title}. Содержимое: </b>"
                                else:

                                    db_user: UserModel = get_user(message1.from_user_id)

                                    if db_user:
                                        text = f"<b>🗑 Удалено сообщение в чате {entity.title} от {(db_user.first_name + ' ' + db_user.last_name) if db_user.last_name else db_user.first_name}. Содержимое: </b>"
                                    else:
                                        text = f"<b>🗑 Удалено сообщение в чате {entity.title}. Содержимое: </b>"

                            elif isinstance(entity, Chat):

                                if not user_settings.save_chat:
                                    return

                                text = f"<b>🗑 Удалено сообщение в группе {entity.title}. Содержимое: </b>"

                            if message1.text:
                                text += f"{message1.text}"

                            text = text.strip()

                            if message1.sticker:
                                await self.bot.send_message(entity=self.user_data.user_id, message=text,
                                                            parse_mode="HTML")
                                # отправка стикера

                                await self.bot.send_file(entity=self.user_data.user_id,
                                                         file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{message.media}")

                                return

                            if message1.media:

                                if text:
                                    await self.bot.send_file(entity=self.user_data.user_id, caption=text,
                                                             file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{message.media}",
                                                             parse_mode="HTML", voice_note=message1.voice_note)
                                else:
                                    await self.bot.send_file(entity=self.user_data.user_id,
                                                             file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{message.media}",
                                                             parse_mode="HTML", voice_note=message1.voice_note)

                            else:
                                await self.bot.send_message(entity=self.user_data.user_id, message=text,
                                                            parse_mode="HTML")

                await event.edit(f"😺 Сообщения успешно восстановлены. Они будут доставлены в "
                                 "ЛС бота в ближайшее время.", parse_mode="HTML")

            return

        first_name = sender.first_name if hasattr(sender, "first_name") else sender.title
        last_name = sender.last_name if hasattr(sender, "last_name") else None

        get_or_create_user(sender.id, first_name, last_name, username=sender.username)

        if event.chat_id in get_all_whitelist_ids():
            return

        # проверка на самоуничтожающиеся сообщения
        if config.SAVE_SD:

            if isinstance(message.media, MessageMediaPhoto):

                sender = event.sender

                if message.media.ttl_seconds:
                    import time

                    file_path = f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{time.time()}-sd.png"

                    await self.client.download_media(message=message, file=file_path)

                    await self.bot.send_file(entity=self.user_data.user_id,
                                             caption=f"<b>🔥 {sender.first_name} отправил вам самоуничтожающееся "
                                                     f"медиа</b>",
                                             file=file_path, parse_mode="HTML")

        if message.message:
            text = message.message

        voice_note: bool = False
        sticker: bool = False
        saved_message_id: Union[int, None] = None

        if message.media:
            import time
            media = str(time.time())

            if isinstance(message.media, MessageMediaPhoto):

                media += ".png"

            elif isinstance(message.media, MessageMediaDocument):

                for attribute in message.media.document.attributes:

                    if isinstance(attribute, DocumentAttributeAudio):
                        voice_note = attribute.voice

                        if voice_note:
                            media += ".ogg"
                        else:
                            media += ".mp3"

                        break

                    elif isinstance(attribute, DocumentAttributeSticker):
                        media += ".tgs"
                        sticker = True

                        # todo: forward, and forward sticker from own dialog
                        # await self.bot.forward_messages(6818240766, from_peer=message.peer_id, messages=[message.id])

                        break

                    elif isinstance(attribute, (DocumentAttributeVideo, DocumentAttributeAnimated)):
                        media += ".mp4"
                        break

                    else:
                        if hasattr(attribute, 'file_name'):
                            media += "." + str(attribute.file_name.split(".")[-1])
                            break

            await self.client.download_media(message=message, file=f"{PATHS.get('FILE_DOWNLOAD_DIR')}{os.sep}{media}")

        create_message(text=text, voice_note=voice_note, sticker=sticker, media=media, message_id=message.id,
                       from_user_id=sender.id, saved_message_id=saved_message_id)

    async def register_handlers(self) -> None:
        """ Register handlers """

        self.client.add_event_handler(self.handle_delete_message, events.MessageDeleted())
        self.client.add_event_handler(self.handle_edit_message, events.MessageEdited())
        self.client.add_event_handler(self.new_message, events.NewMessage())

        async with self.client as cl:
            cl: TelegramClient
            await cl.run_until_disconnected()

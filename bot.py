import logging
import sys
import requests
import traceback
from os import getenv

from aiohttp import web

from aiogram import Bot, Dispatcher, Router, types
from aiogram.enums import ParseMode
from aiogram.filters import CommandStart
from aiogram.types import Message
from aiogram.utils.markdown import hbold
from aiogram.webhook.aiohttp_server import SimpleRequestHandler, setup_application

from PIL import Image

# Bot token can be obtained via https://t.me/BotFather
TOKEN = getenv("BOT_TOKEN")
# Llama backend URL, example: https://da99-195-250-47-71.ngrok-free.app/v1/chat/completions
BACKEND = getenv("BACKEND")

# Webserver settings
# bind localhost only to prevent any external access
WEB_SERVER_HOST = "::"
# Port for incoming request from reverse proxy. Should be any available port
WEB_SERVER_PORT = 8350

# Path to webhook route, on which Telegram will send requests
WEBHOOK_PATH = "/bot/"
# Secret key to validate requests from Telegram (optional)
WEBHOOK_SECRET = "my-secret"
# Base URL for webhook will be used to generate webhook URL for Telegram,
# in this example it is used public DNS with HTTPS support
BASE_WEBHOOK_URL = "https://volovyk.alwaysdata.net/"

# All handlers should be attached to the Router (or Dispatcher)
router = Router()


@router.message(CommandStart())
async def command_start_handler(message: Message) -> None:
    """
    This handler receives messages with `/start` command
    """
    # Most event objects have aliases for API methods that can be called in events' context
    # For example if you want to answer to incoming message you can use `message.answer(...)` alias
    # and the target chat will be passed to :ref:`aiogram.methods.send_message.SendMessage`
    # method automatically or call API method directly via
    # Bot instance: `bot.send_message(chat_id=message.chat.id, ...)`
    await message.answer(f"Привіт, {hbold(message.from_user.full_name)}!")


@router.message()
async def echo_handler(message: types.Message) -> None:
    """
    Handler will forward receive a message back to the sender

    By default, message handler will handle all message types (like text, photo, sticker etc.)
    """
    if message.photo:
        await message.chat.do("typing")
        print("Received an image")
        photo = message.photo[-1]

        # Download the image using the Telegram file ID
        file_id = photo.file_id
        file = await message.bot.get_file(file_id)
        image_url = file.file_path
        await message.bot.download_file(image_url, file_id)

        # Perform color analysis and generate a palette
        palette = generate_palette(file_id)

        # Reply with the generated color palette
        await message.answer("Генерація палітри кольорів:")

        for color in palette:
            await message.answer(f"[#{color}](https://www.color-hex.com/color/{color})", parse_mode=ParseMode.MARKDOWN,
                                 disable_web_page_preview=True)

        return

    try:
        print("Received message: " + message.text)
        # await message.answer("Received input: " + message.text)
        await message.chat.do("typing")

        # Specify the appropriate header for the POST request
        headers = {'Content-type': 'application/json; charset=UTF-8'}
        # Specify the JSON data we want to send data = '{"messages": [{"content": "You are a helpful assistant.",
        # "role": "system"},{"content": "What is the capital of France?","role": "user"}]}'
        data = '{"max_tokens": 200, "messages": [{"content": "You are a helpful assistant.","role": "system"},{"content": "' + message.text + '","role": "user"}]}'

        # Encode the data as UTF-8
        data = data.encode('utf-8')

        # await message.answer("Data = " + data)

        response = requests.post(BACKEND, headers=headers, data=data)

        print("backend response status code: " + str(response.status_code))
        # await message.answer("Backend response status code: " + str(response.status_code))

        # await message.answer("Full response: " + response.text)

        if 200 <= response.status_code <= 299:
            response_dict = response.json()
            await message.answer(response_dict["choices"][0]["message"]["content"], disable_web_page_preview=True)
        else:
            await message.answer("Request to backend was not successful")
    except TypeError as error:
        # But not all the types is supported to be copied so need to handle it
        await message.answer("Error happened! " + traceback.format_exc())


def generate_palette(image_path, num_colors=5):
    image = Image.open(image_path)
    image = image.convert("RGB")

    # Resize the image to reduce processing time
    image = image.resize((100, 100))

    # Get the most dominant colors
    colors = image.getcolors(image.size[0] * image.size[1])
    dominant_colors = sorted(colors, key=lambda x: -x[0])[:num_colors]

    # Extract RGB values
    rgb_colors = [color[1] for color in dominant_colors]

    # Convert RGB to HEX
    hex_colors = ['{:02x}{:02x}{:02x}'.format(r, g, b) for (r, g, b) in rgb_colors]

    return hex_colors


async def on_startup(bot: Bot) -> None:
    # If you have a self-signed SSL certificate, then you will need to send a public
    # certificate to Telegram
    await bot.set_webhook(f"{BASE_WEBHOOK_URL}{WEBHOOK_PATH}", secret_token=WEBHOOK_SECRET)


def main() -> None:
    # Dispatcher is a root router
    dp = Dispatcher()
    # ... and all other routers should be attached to Dispatcher
    dp.include_router(router)

    # Register startup hook to initialize webhook
    dp.startup.register(on_startup)

    # Initialize Bot instance with a default parse mode which will be passed to all API calls
    bot = Bot(TOKEN, parse_mode=ParseMode.HTML)

    # Create aiohttp.web.Application instance
    app = web.Application()

    # Create an instance of request handler,
    # aiogram has few implementations for different cases of usage
    # In this example we use SimpleRequestHandler which is designed to handle simple cases
    webhook_requests_handler = SimpleRequestHandler(
        dispatcher=dp,
        bot=bot,
        secret_token=WEBHOOK_SECRET,
    )
    # Register webhook handler on application
    webhook_requests_handler.register(app, path=WEBHOOK_PATH)

    # Mount dispatcher startup and shutdown hooks to aiohttp application
    setup_application(app, dp, bot=bot)

    # And finally start webserver
    web.run_app(app, host=WEB_SERVER_HOST, port=WEB_SERVER_PORT)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, stream=sys.stdout)
    main()

package main

import (
	"context"
	"fmt"
	"log"
	"log/syslog"
	"math"
	"os"
	"regexp"
	"strconv"
	"strings"

	"github.com/SevereCloud/vksdk/v2/api"
	"github.com/SevereCloud/vksdk/v2/events"
	longpoll "github.com/SevereCloud/vksdk/v2/longpoll-bot"
	"github.com/SevereCloud/vksdk/v2/object"

	tgbotapi "github.com/go-telegram-bot-api/telegram-bot-api/v5"
)

func getEnv(envName string) string {
	value, present := os.LookupEnv(envName)
	if !present {
		log.Panic("variable not set: " + envName)
	}
	return value
}

func getIntEnv(envName string) int64 {
	value, err := strconv.ParseInt(getEnv(envName), 10, 64)
	if err != nil {
		log.Panic("invalid variable: " + envName)
	}
	return value
}

var (
	TG_TOKEN     string = getEnv("TG_TOKEN")
	TG_CHAT_ID   int64  = getIntEnv("TG_CHAT_ID")
	VK_API_TOKEN string = getEnv("VK_API_TOKEN")

	Logger *syslog.Writer

	vk    *api.VK
	lp    *longpoll.LongPoll
	tgbot *tgbotapi.BotAPI

	exceptionTags []string = []string{"#Comics", "#Video", "#AD"}
)

func hasExceptionTag(text string) bool {
	Logger.Notice(fmt.Sprintf("hasExceptionTag: text='%s'\n", text))

	result := false
	for _, tag := range exceptionTags {
		if strings.Contains(text, tag) {
			result = true
			break
		}
	}

	Logger.Notice(fmt.Sprintf("hasExceptionTag returned %v\n", result))
	return result
}

func extractCaption(text string, sourceName string, sourceLink string) string {
	Logger.Notice(fmt.Sprintf("extractCaption: text='%s', sourceName='%s', sourceLink='%s'\n", text, sourceName, sourceLink))

	result := ""
	re := regexp.MustCompile("(#.*?)@")
	matches := re.FindAllStringSubmatch(text, -1)
	for _, match := range matches {
		result += match[1] + " "
	}
	result = strings.TrimRight(result, " \t\n")
	if sourceLink != "" {
		result += "\n\n<a href='" + sourceLink + "'>" + sourceName + "</a>"
	}

	Logger.Notice(fmt.Sprintf("extractCaption returned '%s'\n", result))
	return result
}

func newLinkKeyboard(name string, link string) interface{} {
	Logger.Notice(fmt.Sprintf("newLinkKeyboard: name='%s', link='%s'\n", name, link))

	if len(name) == 0 || len(link) == 0 {
		log.Printf("newLinkKeyboard returned nil")
		return nil
	}
	res := tgbotapi.NewInlineKeyboardMarkup(
		tgbotapi.NewInlineKeyboardRow(
			tgbotapi.NewInlineKeyboardButtonURL(name, link)))

	Logger.Notice(fmt.Sprintf("newLinkKeyboard returned"))
	return res
}

func getBestURL(sizes []object.PhotosPhotoSizes) string {
	var maxWidth float64 = 0
	for _, sz := range sizes {
		maxWidth = math.Max(maxWidth, sz.Width)
	}
	for _, sz := range sizes {
		if sz.Width == maxWidth {
			return sz.URL
		}
	}
	Logger.Warning(fmt.Sprintf("Could not find bestURL out of %v", sizes))
	return ""
}

func wallPostNewHandler(ctx context.Context, obj events.WallPostNewObject) {
	Logger.Notice(fmt.Sprintf("wallPostNewHandler\n"))

	if hasExceptionTag(obj.Text) {
		Logger.Info("skipping post with exception tag: text=" + obj.Text)
		return
	}
	var mediaGroup []interface{}
	for _, att := range obj.Attachments {
		if att.Type == "photo" {
			file := tgbotapi.FileURL(getBestURL(att.Photo.Sizes))
			if len(obj.Attachments) == 1 {
				msg := tgbotapi.NewPhoto(TG_CHAT_ID, file)
				msg.Caption = extractCaption(obj.Text, "", "")
				msg.ReplyMarkup = newLinkKeyboard(obj.Copyright.Name, obj.Copyright.Link)
				tgbot.Send(msg)
				Logger.Info("sended post with img: " + msg.Caption)
			} else {
				media := tgbotapi.NewInputMediaPhoto(file)
				if mediaGroup == nil {
					media.BaseInputMedia.ParseMode = "HTML"
					media.BaseInputMedia.Caption = extractCaption(obj.Text, obj.Copyright.Name, obj.Copyright.Link)
				}
				mediaGroup = append(mediaGroup, media)
			}
		} else if att.Type == "doc" {
			file := tgbotapi.FileURL(att.Doc.URL)
			msg := tgbotapi.NewDocument(TG_CHAT_ID, file)
			msg.Caption = extractCaption(obj.Text, "", "")
			msg.ReplyMarkup = newLinkKeyboard(obj.Copyright.Name, obj.Copyright.Link)
			tgbot.Send(msg)
			Logger.Info("sended post with doc: " + msg.Caption)
		}
	}

	if len(mediaGroup) > 1 {
		Logger.Info(fmt.Sprintf("media collected: %v", mediaGroup))
		msg := tgbotapi.NewMediaGroup(TG_CHAT_ID, mediaGroup)
		tgbot.Send(msg)
	}

	Logger.Notice(fmt.Sprintf("wallPostNewHandler returned"))
}

func initVkApi() {
	vk = api.NewVK(VK_API_TOKEN)
	if vk == nil {
		log.Panic("invalid VK_API_TOKEN")
	}
}

func initVkLongPoll() {
	group, err := vk.GroupsGetByID(nil)
	if err != nil {
		log.Fatal(err)
	}

	lp, err = longpoll.NewLongPoll(vk, group[0].ID)
	if err != nil {
		log.Fatal(err)
	}
	lp.WallPostNew(wallPostNewHandler)
}

func initTgBot() {
	var err error
	tgbot, err = tgbotapi.NewBotAPI(TG_TOKEN)
	if err != nil {
		log.Println("invalid TG_TOKEN")
		log.Fatal(err)
	}
	tgbot.Debug = true
}

func main() {
	var err error

	Logger, err = syslog.New(syslog.LOG_NOTICE, "vktgbot")
	if err != nil {
		log.Fatal(err)
	}

	Logger.Warning("starting bot...")
	fmt.Printf("starting......\n")

	initVkApi()
	initVkLongPoll()
	initTgBot()

	Logger.Info("bot started successfully")

	err = lp.Run()
	if err != nil {
		log.Fatal(err)
	}

	Logger.Info("bot shut down")
}

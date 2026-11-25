-- getVoiceMailAlphaTag() / hidden getVoiceMailAlphaTag(subId). The argument is a subscription id,
-- not a slot, so fall back to the first SIM when there is no value for that index.
function after(hook, param)
    local res = param:getResult()
    local index = param:tryGetArgumentInt(0, 0)
    local nmeName = "cell.voice.mail.alpha.tag." .. tostring(index + 1)
    local fake = param:getSetting(nmeName)
    if param:stringIsEmpty(fake) then
        nmeName = "cell.voice.mail.alpha.tag.1"
        fake = param:getSetting(nmeName)
    end
    if param:stringIsEmpty(fake) then
        return false
    end
    if not param:isForceSetting(nmeName, res) then
        return false
    end

    param:setResult(fake)
    return true, param:safe(res), param:safe(fake)
end

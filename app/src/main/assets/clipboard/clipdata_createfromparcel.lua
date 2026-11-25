function after(hook, param)
    local result = param:getResult()
    if result == null or result:getItemCount() == 0 then
        return false
    end
    local label = param:getSetting("clipboard.label")
    local contents = param:getSetting("clipboard.contents")
    if label == nil then
        label = "Identra"
    end
    if contents == nil then
        contents = "Private"
    end
    local fake = result:newPlainText(label, contents)
    param:setResult(fake)
    return true
end

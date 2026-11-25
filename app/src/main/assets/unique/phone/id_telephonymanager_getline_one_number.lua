-- First configured SIM value wins; never replace the real value with nil.
function after(hook, param)
    local res = param:getResult()
    for i = 1, 2 do
        local nmeName = "cell.unique.phone.number." .. i
        local fake = param:getSetting(nmeName)
        if not param:stringIsEmpty(fake) and param:isForceSetting(nmeName, res) then
            param:setResult(fake)
            return true, param:safe(res), param:safe(fake)
        end
    end
    return false
end

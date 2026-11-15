-- 0.7.4: LinkProperties of the active network name the VPN tunnel (tun0) while the other VPN
-- hooks report a plain mobile network. Answer with the mobile data interface instead.
function after(hook, param)
    local ret = param:getResult()
    if ret == nil then
        return false
    end

    local context = param:getApplicationContext()
    local selfcall = param:getValue('VD.VPN.selfCall.name.' .. param:getPackageName(), context)
    if selfcall ~= nil and selfcall == '1' then
        return false
    end

    if string.find(ret, "^tun%d*$") or string.find(ret, "^pptp%d*$") or string.find(ret, "^ppp%d*$") or string.find(ret, "^ipsec") then
        param:setResult("rmnet_data0")
        return true, ret, "rmnet_data0"
    end
    return false
end
